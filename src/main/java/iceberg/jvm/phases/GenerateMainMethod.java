package iceberg.jvm.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.ir.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class GenerateMainMethod implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        var init = new CompilationUnit.Method();
        init.flags
            = CompilationUnit.Method.AccessFlags.ACC_PUBLIC.value
            | CompilationUnit.Method.AccessFlags.ACC_STATIC.value;
        init.name = unit.constantPool.computeUtf8("main");
        init.descriptor = unit.constantPool.computeUtf8("([Ljava/lang/String;)V");

        init.attributes.add(createCodeAttribute(file, unit));

        unit.methods.add(init);
    }

    private CompilationUnit.CodeAttribute createCodeAttribute(
        IcebergParser.FileContext file, CompilationUnit unit
    ) {
        var attribute = new CompilationUnit.CodeAttribute();
        attribute.attributeName = unit.constantPool.computeUtf8("Code");
        attribute.maxStack = 100; //TODO: how to evaluate?
        attribute.maxLocals = 100; //TODO: how to evaluate?

        attribute.body = (IrBody) file.accept(new IcebergBaseVisitor<IR>() {

            private final List<Map<String, IrVariable>> scopes = new ArrayList<>();

            @Override
            public IR visitFile(IcebergParser.FileContext ctx) {
                scopes.add(new HashMap<>());

                var irBody = new IrBody();
                for (var statement : ctx.statement()) {
                    irBody.statements.add(statement.accept(this));
                }
                irBody.statements.add(new IrReturn());

                return irBody;
            }

            @Override
            public IR visitBlock(IcebergParser.BlockContext ctx) {
                scopes.add(new HashMap<>());

                var irBody = new IrBody();
                for (var statement : ctx.statement()) {
                    irBody.statements.add(statement.accept(this));
                }

                scopes.removeLast();

                return irBody;
            }

            @Override
            public IR visitPrintStatement(IcebergParser.PrintStatementContext ctx) {
                var system = unit.constantPool.computeKlass(
                    unit.constantPool.computeUtf8("java/lang/System")
                );
                var out = unit.constantPool.computeNameAndType(
                    unit.constantPool.computeUtf8("out"),
                    unit.constantPool.computeUtf8("Ljava/io/PrintStream;")
                );
                var field = unit.constantPool.computeFieldRef(system, out);

                var argument = (IrExpression) ctx.expression().accept(this);
                var printStream = unit.constantPool.computeKlass(
                    unit.constantPool.computeUtf8("java/io/PrintStream")
                );
                var println = unit.constantPool.computeNameAndType(
                    unit.constantPool.computeUtf8("println"),
                    unit.constantPool.computeUtf8(
                        Map.of(
                            IcebergType.i32, "(I)V",
                            IcebergType.i64, "(J)V",
                            IcebergType.bool, "(Z)V",
                            IcebergType.string, "(Ljava/lang/String;)V"
                        ).get(argument.type)
                    )
                );
                var method = unit.constantPool.computeMethodRef(printStream, println);
                return new IrStaticCall(field, method, argument);
            }

            @Override
            public IR visitAdditionExpression(IcebergParser.AdditionExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                IcebergType result;
                if (left.type == IcebergType.i64 && right.type == IcebergType.i64) {
                    result = IcebergType.i64;
                } else if (left.type == IcebergType.i64 && right.type == IcebergType.i32) {
                    result = IcebergType.i64;
                } else if (left.type == IcebergType.i32 && right.type == IcebergType.i64) {
                    result = IcebergType.i64;
                } else if (left.type == IcebergType.i32 && right.type == IcebergType.i32) {
                    result = IcebergType.i32;
                } else {
                    throw new SemanticException();
                }

                var operator = ctx.PLUS() != null
                    ? IcebergBinaryOperator.PLUS
                    : IcebergBinaryOperator.SUB;
                return new IrBinaryExpression(left, right, operator, result);
            }

            @Override
            public IR visitDefStatement(IcebergParser.DefStatementContext ctx) {
                var name = ctx.name.getText();
                for (var scope : scopes) {
                    if (scope.containsKey(name)) {
                        throw new SemanticException("'%s' is already defined".formatted(name));
                    }
                }

                if (ctx.expression() != null) {
                    var initializer = (IrExpression) ctx.expression().accept(this);

                    if (ctx.type != null) {
                        var specifiedType = IcebergType.valueOf(ctx.type.getText());
                        if (specifiedType == IcebergType.i64 && initializer.type == IcebergType.i32) {
                            initializer = new IrCast(initializer, IcebergType.i64);
                        } else if (specifiedType != initializer.type) {
                            throw new SemanticException();
                        }
                    }

                    var variable = new IrVariable(initializer.type, initializer);

                    var scope = scopes.getLast();
                    scope.put(name, variable);

                    return variable;
                }

                throw new SemanticException();
            }

            @Override
            public IR visitMultiplicationExpression(IcebergParser.MultiplicationExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                IcebergType result;
                if (left.type == IcebergType.i64 && right.type == IcebergType.i64) {
                    result = IcebergType.i64;
                } else if (left.type == IcebergType.i64 && right.type == IcebergType.i32) {
                    result = IcebergType.i64;
                } else if (left.type == IcebergType.i32 && right.type == IcebergType.i64) {
                    result = IcebergType.i64;
                } else if (left.type == IcebergType.i32 && right.type == IcebergType.i32) {
                    result = IcebergType.i32;
                } else {
                    throw new SemanticException();
                }

                var operator = ctx.STAR() != null
                    ? IcebergBinaryOperator.MULT
                    : IcebergBinaryOperator.DIV;
                return new IrBinaryExpression(left, right, operator, result);
            }

            @Override
            public IR visitRelationalExpression(IcebergParser.RelationalExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                var integers = Set.of(IcebergType.i32, IcebergType.i64);
                if (integers.contains(left.type) && integers.contains(right.type)) {
                    IcebergBinaryOperator operator;
                    if (ctx.GE() != null) operator = IcebergBinaryOperator.LE;
                    else if (ctx.GT() != null) operator = IcebergBinaryOperator.LT;
                    else if (ctx.LE() != null) operator = IcebergBinaryOperator.LE;
                    else if (ctx.LT() != null) operator = IcebergBinaryOperator.LT;
                    else throw new IllegalArgumentException();

                    if (ctx.LT() != null || ctx.LE() != null) {
                        return new IrBinaryExpression(left, right, operator, IcebergType.bool);
                    } else {
                        return new IrBinaryExpression(right, left, operator, IcebergType.bool);
                    }
                } else {
                    throw new SemanticException();
                }
            }

            @Override
            public IR visitEqualityExression(IcebergParser.EqualityExressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                if (left.type == IcebergType.string && right.type == IcebergType.string) {
                    var string = unit.constantPool.computeKlass(
                        unit.constantPool.computeUtf8("java/lang/String")
                    );
                    var equals = unit.constantPool.computeNameAndType(
                        unit.constantPool.computeUtf8("equals"),
                        unit.constantPool.computeUtf8("(Ljava/lang/Object;)Z")
                    );
                    var method = unit.constantPool.computeMethodRef(string, equals);

                    var call = new IrMethodCall(method, IcebergType.bool, left, right);
                    if (ctx.EQ() != null) {
                        return call;
                    }

                    return new IrUnaryExpression(call, IcebergUnaryOperator.NOT, IcebergType.bool);
                }

                var integers = Set.of(IcebergType.i32, IcebergType.i64);
                if (left.type == right.type || integers.containsAll(Set.of(left.type, right.type))) {
                    var binary = new IrBinaryExpression(left, right, IcebergBinaryOperator.EQ, IcebergType.bool);
                    if (ctx.EQ() != null) {
                        return binary;
                    }

                    return new IrUnaryExpression(binary, IcebergUnaryOperator.NOT, IcebergType.bool);
                } else {
                    throw new SemanticException();
                }
            }

            @Override
            public IR visitUnaryMinusExpression(IcebergParser.UnaryMinusExpressionContext ctx) {
                var value = (IrExpression) ctx.atom().accept(this);
                if (value.type == IcebergType.i32 || value.type == IcebergType.i64) {
                    return new IrUnaryExpression(value, IcebergUnaryOperator.MINUS, value.type);
                } else {
                    throw new SemanticException();
                }
            }

            @Override
            public IR visitNegateExpression(IcebergParser.NegateExpressionContext ctx) {
                var value = (IrExpression) ctx.atom().accept(this);
                if (value.type == IcebergType.bool) {
                    return new IrUnaryExpression(value, IcebergUnaryOperator.NOT, value.type);
                } else {
                    throw new SemanticException();
                }
            }

            @Override
            public IR visitLogicalOrExpression(IcebergParser.LogicalOrExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                if (left.type == IcebergType.bool && right.type == IcebergType.bool) {
                    return new IrBinaryExpression(left, right, IcebergBinaryOperator.OR, IcebergType.bool);
                } else {
                    throw new SemanticException();
                }
            }

            @Override
            public IR visitLogicalAndExpression(IcebergParser.LogicalAndExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                if (left.type == IcebergType.bool && right.type == IcebergType.bool) {
                    return new IrBinaryExpression(left, right, IcebergBinaryOperator.AND, IcebergType.bool);
                } else {
                    throw new SemanticException();
                }
            }

            @Override
            public IR visitTerminal(TerminalNode node) {
                return switch (node.getSymbol().getType()) {
                    case IcebergLexer.NUMBER -> new IrNumber(Long.parseLong(node.getText()));
                    case IcebergLexer.FALSE -> new IrBool(false);
                    case IcebergLexer.TRUE -> new IrBool(true);
                    case IcebergLexer.STRING -> {
                        var text = node.getText();
                        var string = text
                            .substring(1, text.length() - 1)
                            .replace("\\\"", "\"")
                            .replace("\\n", "\n");
                        yield new IrString(unit.constantPool.computeString(string));
                    }
                    case IcebergLexer.ID -> {
                        var name = node.getSymbol().getText();
                        for (int i = scopes.size() - 1; i >= 0; i--) {
                            var scope = scopes.get(i);
                            if (scope.containsKey(name)) {
                                yield new IrReadVariable(scope.get(name));
                            }
                        }

                        throw new SemanticException("'%s' is not defined".formatted(name));
                    }
                    default -> null;
                };
            }

            @Override
            protected IR aggregateResult(IR aggregate, IR nextResult) {
                return nextResult != null ? nextResult : aggregate;
            }
        });

        return attribute;
    }
}
