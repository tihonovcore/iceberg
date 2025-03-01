package iceberg.jvm.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.ir.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.stream.Collectors;

public class BuildIrTreePhase implements CompilationPhase {

    record FunctionDescriptor(
        String functionName,
        List<IcebergType> parametersTypes
    ) {}

    Map<FunctionDescriptor, IrFunction> findAllFunctions(IcebergParser.FileContext file) {
        var functions = new HashMap<FunctionDescriptor, IrFunction>();
        file.accept(new IcebergBaseVisitor<Void>() {

            @Override
            public Void visitClassDefinitionStatement(IcebergParser.ClassDefinitionStatementContext ctx) {
                return null; //these functions are in different scope
            }

            @Override
            public Void visitFunctionDefinitionStatement(
                IcebergParser.FunctionDefinitionStatementContext ctx
            ) {
                var parametersTypes = ctx.parameters().parameter().stream()
                    .map(parameter -> parameter.type.getText())
                    //TODO: support classes
                    .map(IcebergType::valueOf)
                    .toList();
                var descriptor = new FunctionDescriptor(ctx.name.getText(), parametersTypes);

                if (functions.containsKey(descriptor)) {
                    throw new SemanticException("function already exists");
                }

                var functionName = ctx.name.getText();
                var returnType = ctx.returnType == null
                    ? IcebergType.unit
                    : IcebergType.valueOf(ctx.returnType.getText());
                var function = new IrFunction(functionName, returnType);

                ctx.parameters().parameter().stream()
                    .map(parameter -> IcebergType.valueOf(parameter.type.getText()))
                    .map(parameter -> new IrVariable(parameter, null))
                    .forEach(function.parameters::add);

                functions.put(descriptor, function);

                return null;
            }
        });

        return functions;
    }

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        var functions = findAllFunctions(file);
        //TODO: возможно нужен findAllClasses(file);

        unit.irFile = (IrFile) file.accept(new IcebergBaseVisitor<IR>() {

            private final List<Map<String, IrVariable>> scopes = new ArrayList<>();

            @Override
            public IR visitFile(IcebergParser.FileContext ctx) {
                var userDefinedClasses = ctx.statement().stream()
                    .map(IcebergParser.StatementContext::classDefinitionStatement)
                    .filter(Objects::nonNull)
                    .map(irClass -> (IrClass) irClass.accept(this))
                    .toList();
                var userDefinedFunctions = ctx.statement().stream()
                    .map(IcebergParser.StatementContext::functionDefinitionStatement)
                    .filter(Objects::nonNull)
                    .map(function -> (IrFunction) function.accept(this))
                    .toList();

                var mainFunctionStatements = ctx.statement().stream()
                    .filter(statement -> statement.functionDefinitionStatement() == null)
                    .filter(statement -> statement.classDefinitionStatement() == null)
                    .toList();
                var mainFunction = buildMainFunction(mainFunctionStatements);

                var irFile = new IrFile();
                irFile.classes.addAll(userDefinedClasses);
                irFile.functions.addAll(userDefinedFunctions);
                irFile.functions.add(mainFunction);

                return irFile;
            }

            private IrFunction buildMainFunction(List<IcebergParser.StatementContext> statements) {
                scopes.add(new HashMap<>());

                var functionName = "main";
                var mainFunction = new IrFunction(functionName, IcebergType.unit);
                for (var statement : statements) {
                    mainFunction.irBody.statements.add(statement.accept(this));
                }
                mainFunction.irBody.statements.add(new IrReturn());

                return mainFunction; //TODO: fill parameters??
            }

            @Override
            public IR visitClassDefinitionStatement(IcebergParser.ClassDefinitionStatementContext ctx) {
                var irClass = new IrClass(ctx.name.getText());

                ctx.defStatement().stream()
                    .map(definition -> (IrVariable) definition.accept(this))
                    .forEach(irClass.fields::add);
                ctx.functionDefinitionStatement().stream()
                    .map(definition -> (IrFunction) definition.accept(this))
                    .forEach(irClass.methods::add);

                return irClass;
            }

            @Override
            public IR visitFunctionDefinitionStatement(IcebergParser.FunctionDefinitionStatementContext ctx) {
                var parametersTypes = ctx.parameters().parameter().stream()
                    .map(parameter -> parameter.type.getText())
                    //TODO: support classes
                    .map(IcebergType::valueOf)
                    .toList();
                var descriptor = new FunctionDescriptor(ctx.name.getText(), parametersTypes);

                var irFunction = functions.get(descriptor);

                scopes.add(new HashMap<>());

                for (int i = 0; i < ctx.parameters().parameter().size(); i++) {
                    var parameter = ctx.parameters().parameter().get(i);
                    var irParameter = irFunction.parameters.get(i);

                    scopes.getLast().put(parameter.name.getText(), irParameter);
                }

                irFunction.irBody.statements.addAll(
                    ((IrBody) ctx.block().accept(this)).statements
                );

                if (irFunction.returnType == IcebergType.unit) {
                    irFunction.irBody.statements.add(new IrReturn());
                }

                scopes.removeLast();

                return irFunction;
            }

            //TODO: проверить что все return из функции одного типа и совпадают с ReturnType
            //TODO: если у функции returnType!=unit - проверить что во всех ветках есть явный return

            @Override
            public IR visitReturnStatement(IcebergParser.ReturnStatementContext ctx) {
                return new IrReturn((IrExpression) ctx.expression().accept(this));
            }

            @Override
            public IR visitFunctionCall(IcebergParser.FunctionCallContext ctx) {
                var arguments = ctx.arguments().expression().stream()
                    .map(arg -> (IrExpression) arg.accept(this))
                    .toList();

                var argumentsTypes = arguments.stream()
                    .map(expr -> expr.type)
                    .toList();
                var descriptor = new FunctionDescriptor(ctx.name.getText(), argumentsTypes);
                if (!functions.containsKey(descriptor)) {
                    throw new SemanticException("function not found");
                }

                var irFunction = functions.get(descriptor);

                var mapping = Map.of(
                    IcebergType.i32, "I",
                    IcebergType.i64, "J",
                    IcebergType.bool, "Z",
                    IcebergType.string, "Ljava/lang/String;",
                    IcebergType.unit, "V"
                );

                var params = irFunction.parameters.stream()
                    .map(v -> v.type)
                    .map(mapping::get)
                    .collect(Collectors.joining(""));

                var descr = "(" + params + ")" + mapping.get(irFunction.returnType);

                var nameAndType = unit.constantPool.computeNameAndType(
                    unit.constantPool.computeUtf8(ctx.name.getText()),
                    unit.constantPool.computeUtf8(descr) //TODO
                );
                var method = unit.constantPool.computeMethodRef(unit.thisRef, nameAndType);

                //TODO: pass irFunction, not MethodRef ??
                return new IrStaticCall(irFunction.returnType, method, arguments.toArray(arguments.toArray(new IrExpression[0])));
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
                return new IrPrint(field, method, argument);
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

                IrExpression initializer = null;
                if (ctx.expression() != null) {
                    initializer = (IrExpression) ctx.expression().accept(this);

                    if (ctx.type != null) {
                        var specifiedType = IcebergType.valueOf(ctx.type.getText());
                        if (specifiedType == IcebergType.i64 && initializer.type == IcebergType.i32) {
                            initializer = new IrCast(initializer, IcebergType.i64);
                        } else if (specifiedType != initializer.type) {
                            throw new SemanticException();
                        }
                    }
                }

                var type = ctx.type != null
                    ? IcebergType.valueOf(ctx.type.getText())
                    : initializer.type;

                var variable = new IrVariable(type, initializer);

                var scope = scopes.getLast();
                scope.put(name, variable);

                return variable;
            }

            @Override
            public IR visitAssignExpression(IcebergParser.AssignExpressionContext ctx) {
                //NOTE: for now only var name is possible
                //TODO: support expressions
                var name = ctx.left.getText();

                IrVariable irVariable = null;
                for (var scope : scopes) {
                    if (scope.containsKey(name)) {
                        irVariable = scope.get(name);
                        break;
                    }
                }

                if (irVariable == null) {
                    throw new SemanticException("'%s' is not defined".formatted(name));
                }

                var expression = (IrExpression) ctx.right.accept(this);
                if (irVariable.type != expression.type) {
                    throw new SemanticException(
                        "cannot assign %s-value to %s::%s".formatted(expression.type, name, irVariable.type)
                    );
                }

                return new IrAssignVariable(irVariable, expression);
            }

            @Override
            public IR visitIfStatement(IcebergParser.IfStatementContext ctx) {
                var condition = (IrExpression) ctx.condition.accept(this);
                if (condition.type != IcebergType.bool) {
                    throw new SemanticException("expected bool");
                }

                var thenStatement = ctx.thenStatement.accept(this);
                var elseStatement = ctx.elseStatement != null
                    ? ctx.elseStatement.accept(this)
                    : null;

                return new IrIfStatement(condition, thenStatement, elseStatement);
            }

            @Override
            public IR visitWhileStatement(IcebergParser.WhileStatementContext ctx) {
                var condition = (IrExpression) ctx.expression().accept(this);
                if (condition.type != IcebergType.bool) {
                    throw new SemanticException("expected bool");
                }

                var body = ctx.statement().accept(this);
                return new IrLoop(condition, body);
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
    }
}
