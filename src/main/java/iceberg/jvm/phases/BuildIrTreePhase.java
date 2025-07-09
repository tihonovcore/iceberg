package iceberg.jvm.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.ir.*;
import iceberg.jvm.ir.IcebergType;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class BuildIrTreePhase {

    public IrFile execute(IcebergParser.FileContext file) {
        var classResolver = new ClassResolver(file);

        return (IrFile) file.accept(new IcebergBaseVisitor<IR>() {

            private final List<Map<String, IrVariable>> scopes = new ArrayList<>();

            @Override
            public IR visitFile(IcebergParser.FileContext ctx) {
                var userDefinedClasses = ctx.statement().stream()
                    .map(IcebergParser.StatementContext::classDefinitionStatement)
                    .filter(Objects::nonNull)
                    .map(irClass -> (IrClass) irClass.accept(this))
                    .toList();

                //NOTE: bind user-defined functions to Iceberg class
                ctx.statement().stream()
                    .map(IcebergParser.StatementContext::functionDefinitionStatement)
                    .filter(Objects::nonNull)
                    .forEach(function -> function.accept(this));

                var mainFunctionStatements = ctx.statement().stream()
                    .filter(statement -> statement.functionDefinitionStatement() == null)
                    .filter(statement -> statement.classDefinitionStatement() == null)
                    .toList();
                var mainFunction = buildMainFunction(mainFunctionStatements);
                classResolver.getIcebergIrClass().methods.add(mainFunction);

                var irFile = new IrFile();
                irFile.classes.addAll(userDefinedClasses);
                irFile.classes.add(classResolver.getIcebergIrClass());

                return irFile;
            }

            private IrFunction buildMainFunction(List<IcebergParser.StatementContext> statements) {
                scopes.add(new HashMap<>());

                var functionName = "main";
                var icebergIrClass = classResolver.getIcebergIrClass();
                var mainFunction = new IrFunction(icebergIrClass, functionName, IcebergType.unit);
                for (var statement : statements) {
                    mainFunction.irBody.statements.add(statement.accept(this));
                }
                mainFunction.irBody.statements.add(new IrReturn());

                return mainFunction; //TODO: fill parameters - ([Ljava/lang/String;)V
            }

            IrClass currentClass = classResolver.getIcebergIrClass();

            @Override
            public IR visitClassDefinitionStatement(IcebergParser.ClassDefinitionStatementContext ctx) {
                var prev = currentClass;
                try {
                    var irClass = classResolver.getIrClass(ctx.name.getText());
                    currentClass = irClass;

                    ctx.defStatement().forEach(definition -> {
                        //TODO: support user types
                        var type = IcebergType.valueOf(definition.type.getText());
                        var fieldName = definition.name.getText();
                        irClass.fields.put(fieldName, new IrField(irClass, fieldName, type));
                    });

                    //todo: надо вызвать findAllFunctions
                    ctx.functionDefinitionStatement().forEach(
                        definition -> definition.accept(this)
                    );

                    return irClass;
                } finally {
                    currentClass = prev;
                }
            }

            @Override
            public IR visitFunctionDefinitionStatement(IcebergParser.FunctionDefinitionStatementContext ctx) {
                var parametersTypes = ctx.parameters().parameter().stream()
                    .map(parameter -> parameter.type.getText())
                    //TODO: support user-defined types
                    .map(IcebergType::valueOf)
                    .toList();

                var irFunction = currentClass
                    .findMethod(ctx.name.getText(), parametersTypes)
                    .orElseThrow(() -> new IllegalStateException("impossible"));

                //TODO: scopes надо перетереть - функция не должна видеть переменные снаружи
                // только поля, но их надо прописать отдельно
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

                //TODO: тут нужно вернуть старые scopes
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
                var optional = currentClass.findMethod(ctx.name.getText(), argumentsTypes);
                if (optional.isEmpty()) {
                    throw new SemanticException("function not found");
                }

                return new IrStaticCall(
                    optional.get(),
                    arguments.toArray(arguments.toArray(new IrExpression[0]))
                );
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
                var argument = (IrExpression) ctx.expression().accept(this);

                var possibleSpecificParametersTypes = Set.of(
                    IcebergType.i32, IcebergType.i64,
                    IcebergType.bool, IcebergType.string
                );
                if (possibleSpecificParametersTypes.contains(argument.type)) {
                    var method = findPrintlnByParameterType(argument.type);
                    return new IrPrint(method, argument);
                }

                var method = findPrintlnByParameterType(IcebergType.object);
                return new IrPrint(method, argument);
            }

            private IrFunction findPrintlnByParameterType(IcebergType parametersType) {
                var printStreamClass = IcebergType.printStream.irClass;
                return printStreamClass.methods.stream()
                    .filter(function -> "println".equals(function.name))
                    .filter(function -> {
                        var single = function.parameters.getFirst();
                        return single.type == parametersType;
                    })
                    .findFirst().orElseThrow();
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
                var left = ctx.left.accept(this);
                if (left instanceof IrGetField irGetField) {
                    var expression = (IrExpression) ctx.right.accept(this);
                    if (irGetField.type != expression.type) {
                        throw new SemanticException("bad type");
                    }

                    return new IrPutField(irGetField.receiver, irGetField.irField, expression);
                }

                //TODO: use `left`?
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
                    var stringIrClass = IcebergType.string.irClass;
                    var stringEquals = stringIrClass.methods.stream()
                        .filter(fun -> fun.name.equals("equals"))
                        .findFirst().orElseThrow();

                    var call = new IrMethodCall(stringEquals, left, right);
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
            public IR visitNewExpression(IcebergParser.NewExpressionContext ctx) {
                var className = ctx.className.getText();
                var irClass = classResolver.getIrClass(className);
                if (irClass == null) {
                    throw new SemanticException("class '%s' is not defined".formatted(className));
                }

                //TODO: support parameters?
                return new IrNew(irClass);
            }

            @Override
            public IR visitMemberExpression(IcebergParser.MemberExpressionContext ctx) {
                if (ctx.functionCall() != null) {
                    var receiver = (IrExpression) ctx.expression().accept(this);
                    return methodCall(receiver, ctx.functionCall());
                } else {
                    var fieldName = ctx.ID().getText();
                    var receiver = (IrExpression) ctx.expression().accept(this);
                    var irField = receiver.type.irClass.fields.get(fieldName);

                    //NOTE: если это l-value, то IrGetField заменится на IrPutField
                    return new IrGetField(receiver, irField);
                }
            }

            private IrMethodCall methodCall(
                IrExpression receiver,
                IcebergParser.FunctionCallContext ctx
            ) {
                var arguments = ctx.arguments().expression().stream()
                    .map(arg -> (IrExpression) arg.accept(this))
                    .toList();

                var argumentsTypes = arguments.stream()
                    .map(expr -> expr.type)
                    .toList();

                var optional = receiver.type.irClass.findMethod(ctx.name.getText(), argumentsTypes);
                if (optional.isEmpty()) {
                    throw new SemanticException("function not found");
                }
                var irFunction = optional.get();

                return new IrMethodCall(
                    irFunction,
                    receiver,
                    arguments.toArray(arguments.toArray(new IrExpression[0]))
                );
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
                        yield new IrString(string);
                    }
                    case IcebergLexer.THIS -> new IrThis(currentClass);
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
