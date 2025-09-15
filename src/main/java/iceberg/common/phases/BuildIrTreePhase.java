package iceberg.common.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import iceberg.ir.*;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class BuildIrTreePhase {

    public IrFile execute(IcebergParser.FileContext file) {
        return (IrFile) file.accept(new IcebergBaseVisitor<IR>() {

            private final ClassResolver classResolver = new ClassResolver(file);
            private final List<Map<String, IrVariable>> scopes = new ArrayList<>();

            @Override
            public IR visitFile(IcebergParser.FileContext ctx) {
                var userDefinedClasses = ctx.statement().stream()
                    .map(IcebergParser.StatementContext::classDefinitionStatement)
                    .filter(Objects::nonNull)
                    .map(irClass -> (IrClass) irClass.accept(this))
                    .toList();

                //build bodies for top-level functions
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
                addReturnStatementToMainFunction(mainFunction);

                return mainFunction;
            }

            private void addReturnStatementToMainFunction(IrFunction mainFunction) {
                var statements = mainFunction.irBody.statements;
                if (statements.isEmpty() || !(statements.getLast() instanceof IrReturn)) {
                    statements.add(new IrReturn());
                }
            }

            private IrClass currentClass = classResolver.getIcebergIrClass();

            @Override
            public IR visitClassDefinitionStatement(IcebergParser.ClassDefinitionStatementContext ctx) {
                var prev = currentClass;
                try {
                    var irClass = classResolver.getIrClass(ctx.name.getText());
                    currentClass = irClass;

                    ctx.fieldDefinition().forEach(
                        definition -> definition.accept(this)
                    );
                    ctx.functionDefinitionStatement().forEach(
                        definition -> definition.accept(this)
                    );

                    return irClass;
                } finally {
                    currentClass = prev;
                }
            }

            @Override
            public IR visitFieldDefinition(IcebergParser.FieldDefinitionContext ctx) {
                var fieldName = ctx.name.getText();
                var irField = currentClass.fields.get(fieldName);

                if (ctx.expression() != null) {
                    var initializer = (IrExpression) ctx.expression().accept(this);

                    assertAssignable(irField.type, initializer.type, ctx);
                    irField.initializer = initializer;
                }

                return irField;
            }

            @Override
            public IR visitFunctionDefinitionStatement(IcebergParser.FunctionDefinitionStatementContext ctx) {
                var functionName = ctx.name.getText();
                var parametersTypes = ctx.parameters().parameter().stream()
                    .map(parameter -> parameter.type.getText())
                    .map(classResolver::getIcebergType)
                    .toList();

                var irFunction = currentClass
                    .findMethod(functionName, parametersTypes)
                    .orElseThrow(() -> new IllegalStateException(
                        "impossible, function should have been created at " + ClassResolver.class.getSimpleName()
                    ));

                scopes.add(new HashMap<>());

                for (int i = 0; i < ctx.parameters().parameter().size(); i++) {
                    var parameter = ctx.parameters().parameter().get(i);
                    var irParameter = irFunction.parameters.get(i);

                    scopes.getLast().put(parameter.name.getText(), irParameter);
                }

                irFunction.irBody.statements.addAll(
                    ((IrBody) ctx.block().accept(this)).statements
                );

                //add explicit return if needed
                if (irFunction.returnType == IcebergType.unit) {
                    var statements = irFunction.irBody.statements;
                    if (statements.isEmpty() || !(statements.getLast() instanceof IrReturn)) {
                        statements.add(new IrReturn());
                    }
                }

                scopes.removeLast();

                return irFunction;
            }

            @Override
            public IR visitReturnStatement(IcebergParser.ReturnStatementContext ctx) {
                return ctx.expression() != null
                    ? new IrReturn((IrExpression) ctx.expression().accept(this))
                    : new IrReturn(null);
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
                    throw new SemanticException(
                        "function '%s' not found".formatted(ctx.name.getText())
                    );
                }

                return new IrStaticCall(optional.get(), arguments);
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
                if (argument.type.equals(IcebergType.unit)) {
                    throw new SemanticException("impossible print");
                }

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
            public IR visitDefStatement(IcebergParser.DefStatementContext ctx) {
                var name = ctx.name.getText();
                for (var scope : scopes) {
                    if (scope.containsKey(name)) {
                        throw new SemanticException("'%s' is already defined".formatted(name));
                    }
                }

                IrVariable variable;
                if (ctx.expression() == null) {
                    var type = classResolver.getIcebergType(ctx.type.getText());
                    variable = new IrVariable(type, null);
                } else {
                    var initializer = (IrExpression) ctx.expression().accept(this);

                    if (ctx.type != null) {
                        var specifiedType = classResolver.getIcebergType(ctx.type.getText());
                        if (specifiedType == IcebergType.i64 && initializer.type == IcebergType.i32) {
                            initializer = new IrCast(initializer, IcebergType.i64);
                        } else if (specifiedType != initializer.type) {
                            throw new SemanticException(
                                "incompatible types: %s and %s".formatted(
                                    specifiedType.irClass.name,
                                    initializer.type.irClass.name
                                )
                            );
                        }
                    }

                    variable = new IrVariable(initializer.type, initializer);
                }

                var scope = scopes.getLast();
                scope.put(name, variable);

                return variable;
            }

            @Override
            public IR visitAssignExpression(IcebergParser.AssignExpressionContext ctx) {
                var left = ctx.left.accept(this);
                if (left instanceof IrGetField irGetField) {
                    var expression = (IrExpression) ctx.right.accept(this);

                    assertAssignable(irGetField.type, expression.type, ctx);
                    return new IrPutField(irGetField.receiver, irGetField.irField, expression);
                }

                if (left instanceof IrReadVariable irReadVariable) {
                    var irVariable = irReadVariable.definition;
                    var expression = (IrExpression) ctx.right.accept(this);

                    assertAssignable(irVariable.type, expression.type, ctx);
                    return new IrAssignVariable(irVariable, expression);
                }

                throw new IllegalStateException("impossible");
            }

            private static void assertAssignable(
                IcebergType to, IcebergType from, RuleContext ctx
            ) {
                if (!to.equals(from)) {
                    throw new SemanticException("bad type on assign: " + ctx.getText());
                }
            }

            private static void assertIntegers(
                IrExpression left, IrExpression right, RuleContext ctx
            ) {
                var integers = Set.of(IcebergType.i32, IcebergType.i64);
                if (integers.contains(left.type) && integers.contains(right.type)) {
                    return;
                }

                throw new SemanticException("""
                    cannot apply operation to %s and %s
                    at %s""".formatted(left.type, right.type, ctx.getText())
                );
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
            public IR visitAdditionExpression(IcebergParser.AdditionExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);
                var operator = ctx.PLUS() != null
                    ? IcebergBinaryOperator.PLUS
                    : IcebergBinaryOperator.SUB;

                assertIntegers(left, right, ctx);
                return buildArithmeticExpression(left, right, operator);
            }


            @Override
            public IR visitMultiplicationExpression(IcebergParser.MultiplicationExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);
                var operator = ctx.STAR() != null
                    ? IcebergBinaryOperator.MULT
                    : IcebergBinaryOperator.DIV;

                assertIntegers(left, right, ctx);
                return buildArithmeticExpression(left, right, operator);
            }

            private IrBinaryExpression buildArithmeticExpression(
                IrExpression left,
                IrExpression right,
                IcebergBinaryOperator operator
            ) {
                if (left.type.equals(IcebergType.i32) && right.type.equals(IcebergType.i64)) {
                    left = new IrCast(left, IcebergType.i64);
                }

                if (left.type.equals(IcebergType.i64) && right.type.equals(IcebergType.i32)) {
                    right = new IrCast(right, IcebergType.i64);
                }

                return new IrBinaryExpression(left, right, operator, left.type);
            }

            @Override
            public IR visitRelationalExpression(IcebergParser.RelationalExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                assertIntegers(left, right, ctx);

                if (left.type.equals(IcebergType.i32) && right.type.equals(IcebergType.i64)) {
                    left = new IrCast(left, IcebergType.i64);
                }

                if (left.type.equals(IcebergType.i64) && right.type.equals(IcebergType.i32)) {
                    right = new IrCast(right, IcebergType.i64);
                }

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

                    var call = new IrMethodCall(left, stringEquals, right);
                    if (ctx.EQ() != null) {
                        return call;
                    }

                    return new IrUnaryExpression(call, IcebergUnaryOperator.NOT, IcebergType.bool);
                }

                if (left.type.equals(IcebergType.i32) && right.type.equals(IcebergType.i64)) {
                    left = new IrCast(left, IcebergType.i64);
                }

                if (left.type.equals(IcebergType.i64) && right.type.equals(IcebergType.i32)) {
                    right = new IrCast(right, IcebergType.i64);
                }

                if (left.type.equals(right.type)) {
                    var binary = new IrBinaryExpression(left, right, IcebergBinaryOperator.EQ, IcebergType.bool);
                    if (ctx.EQ() != null) {
                        return binary;
                    }

                    return new IrUnaryExpression(binary, IcebergUnaryOperator.NOT, IcebergType.bool);
                } else {
                    throw new SemanticException("""
                        cannot apply operation to %s and %s
                        at %s""".formatted(left.type, right.type, ctx.getText())
                    );
                }
            }

            @Override
            public IR visitUnaryMinusExpression(IcebergParser.UnaryMinusExpressionContext ctx) {
                var value = (IrExpression) ctx.atom().accept(this);
                if (value.type == IcebergType.i32 || value.type == IcebergType.i64) {
                    return new IrUnaryExpression(value, IcebergUnaryOperator.MINUS, value.type);
                } else {
                    throw new SemanticException("""
                        cannot apply operation to %s
                        at %s""".formatted(value.type, ctx.getText())
                    );
                }
            }

            @Override
            public IR visitNegateExpression(IcebergParser.NegateExpressionContext ctx) {
                var value = (IrExpression) ctx.atom().accept(this);
                if (value.type == IcebergType.bool) {
                    return new IrUnaryExpression(value, IcebergUnaryOperator.NOT, value.type);
                } else {
                    throw new SemanticException("""
                        cannot apply operation to %s
                        at %s""".formatted(value.type, ctx.getText())
                    );
                }
            }

            @Override
            public IR visitLogicalOrExpression(IcebergParser.LogicalOrExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                if (left.type == IcebergType.bool && right.type == IcebergType.bool) {
                    return new IrBinaryExpression(left, right, IcebergBinaryOperator.OR, IcebergType.bool);
                } else {
                    throw new SemanticException("""
                        cannot apply operation to %s and %s
                        at %s""".formatted(left.type, right.type, ctx.getText())
                    );
                }
            }

            @Override
            public IR visitLogicalAndExpression(IcebergParser.LogicalAndExpressionContext ctx) {
                var left = (IrExpression) ctx.left.accept(this);
                var right = (IrExpression) ctx.right.accept(this);

                if (left.type == IcebergType.bool && right.type == IcebergType.bool) {
                    return new IrBinaryExpression(left, right, IcebergBinaryOperator.AND, IcebergType.bool);
                } else {
                    throw new SemanticException("""
                        cannot apply operation to %s and %s
                        at %s""".formatted(left.type, right.type, ctx.getText())
                    );
                }
            }

            @Override
            public IR visitNewExpression(IcebergParser.NewExpressionContext ctx) {
                var className = ctx.className.getText();
                if (classResolver.getIrClass(className) != null) {
                    var type = classResolver.getIcebergType(className);
                    return new IrNew(type);
                }

                throw new SemanticException("class '%s' is not defined".formatted(className));
            }

            //TODO: в случае с импортами
            // - не работают статические функции, например, Collections.sort(...)
            // - нет боксинга (нельзя в список засунуть число)
            // - нет работы с наследованием (при поиске метода IrClass понимает,
            //   что в Object можно положить что угодно, но это все)

            @Override
            public IR visitMemberExpression(IcebergParser.MemberExpressionContext ctx) {
                if (ctx.functionCall() != null) {
                    var receiver = (IrExpression) ctx.expression().accept(this);
                    return methodCall(receiver, ctx.functionCall());
                } else {
                    var fieldName = ctx.ID().getText();
                    var receiver = (IrExpression) ctx.expression().accept(this);
                    var irField = receiver.type.irClass.fields.get(fieldName);
                    if (irField == null) {
                        throw new SemanticException(
                            "unknown field '%s' at %s".formatted(fieldName, ctx.getText())
                        );
                    }

                    //NOTE: если выше окажется это l-value,
                    //то IrGetField заменится на IrPutField
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
                    throw new SemanticException(
                        "function '%s' not found".formatted(ctx.name.getText())
                    );
                }
                var irFunction = optional.get();

                return new IrMethodCall(
                    receiver,
                    irFunction,
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
                                //NOTE: если выше окажется что это l-value,
                                //то IrReadVariable заменится на IrAssignVariable
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
