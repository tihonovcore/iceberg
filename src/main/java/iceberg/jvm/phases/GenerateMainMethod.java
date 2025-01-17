package iceberg.jvm.phases;

import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.ir.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Map;

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

            @Override
            public IR visitFile(IcebergParser.FileContext ctx) {
                var irBody = new IrBody();
                for (var statement : ctx.statement()) {
                    irBody.statements.add(statement.accept(this));
                }
                irBody.statements.add(new IrReturn());

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
                    throw new IllegalStateException("not implemented");
                }

                return new IrBinaryExpression(left, right, IcebergBinaryOperator.PLUS, result);
            }

            @Override
            public IR visitUnaryMinusExpression(IcebergParser.UnaryMinusExpressionContext ctx) {
                var value = (IrExpression) ctx.atom().accept(this);
                if (value.type == IcebergType.i32 || value.type == IcebergType.i64) {
                    return new IrUnaryExpression(value, IcebergUnaryOperator.MINUS, value.type);
                } else {
                    throw new IllegalArgumentException();
                }
            }

            @Override
            public IR visitNegateExpression(IcebergParser.NegateExpressionContext ctx) {
                var value = (IrExpression) ctx.atom().accept(this);
                if (value.type == IcebergType.bool) {
                    return new IrUnaryExpression(value, IcebergUnaryOperator.NOT, value.type);
                } else {
                    throw new IllegalArgumentException();
                }
            }

            @Override
            public IR visitLogicalOrExpression(IcebergParser.LogicalOrExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                if (left.type == IcebergType.bool && right.type == IcebergType.bool) {
                    return new IrBinaryExpression(left, right, IcebergBinaryOperator.OR, IcebergType.bool);
                } else {
                    throw new IllegalArgumentException();
                }
            }

            @Override
            public IR visitLogicalAndExpression(IcebergParser.LogicalAndExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                if (left.type == IcebergType.bool && right.type == IcebergType.bool) {
                    return new IrBinaryExpression(left, right, IcebergBinaryOperator.AND, IcebergType.bool);
                } else {
                    throw new IllegalArgumentException();
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
