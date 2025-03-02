package iceberg.jvm.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.ir.*;
import iceberg.jvm.ir.IcebergType;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class BuildIrTreePhase implements CompilationPhase {

    Map<String, IrClass> findAllClasses(IcebergParser.FileContext file) {
        var classes = new HashMap<String, IrClass>();
        file.accept(new IcebergBaseVisitor<>() {
            @Override
            public Object visitClassDefinitionStatement(IcebergParser.ClassDefinitionStatementContext ctx) {
                var name = ctx.name.getText();
                if (classes.containsKey(name)) {
                    throw new SemanticException("class already exists");
                }

                classes.put(name, new IrClass(name));
                return super.visitClassDefinitionStatement(ctx);
            }
        });

        return classes;
    }

    record FunctionDescriptor(
        String functionName,
        List<IcebergType> parametersTypes
    ) {}

    Map<FunctionDescriptor, IrFunction> findAllFunctions(
        IcebergParser.FileContext file,
        Map<String, IrClass> classes
    ) {
        var functions = new HashMap<FunctionDescriptor, IrFunction>();
        file.accept(new IcebergBaseVisitor<Void>() {

            IcebergParser.ClassDefinitionStatementContext currentClass = null;

            @Override
            public Void visitClassDefinitionStatement(IcebergParser.ClassDefinitionStatementContext ctx) {
                var prev = currentClass;
                try {
                    currentClass = ctx;
                    return super.visitClassDefinitionStatement(ctx);
                } finally {
                    currentClass = prev;
                }
            }

            @Override
            public Void visitFunctionDefinitionStatement(
                IcebergParser.FunctionDefinitionStatementContext ctx
            ) {
                var functionName = currentClass != null
                    ? currentClass.name.getText() + "$" + ctx.name.getText()
                    : ctx.name.getText();
                var parametersTypes = ctx.parameters().parameter().stream()
                    .map(parameter -> parameter.type.getText())
                    //TODO: support classes
                    .map(IcebergType::valueOf)
                    .toList();
                var descriptor = new FunctionDescriptor(functionName, parametersTypes);

                if (functions.containsKey(descriptor)) {
                    throw new SemanticException("function already exists");
                }

                var classOwner = currentClass != null
                    ? classes.get(currentClass.name.getText())
                    : IcebergType.iceberg.irClass;
                var returnType = ctx.returnType == null
                    ? IcebergType.unit
                    : IcebergType.valueOf(ctx.returnType.getText());
                var function = new IrFunction(classOwner, functionName, returnType);

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
        var classes = findAllClasses(file);
        var functions = findAllFunctions(file, classes);

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
                var mainFunction = new IrFunction(IcebergType.iceberg.irClass, functionName, IcebergType.unit);
                for (var statement : statements) {
                    mainFunction.irBody.statements.add(statement.accept(this));
                }
                mainFunction.irBody.statements.add(new IrReturn());

                return mainFunction; //TODO: fill parameters??
            }

            IrClass currentClass = null;

            @Override
            public IR visitClassDefinitionStatement(IcebergParser.ClassDefinitionStatementContext ctx) {
                var prev = currentClass;
                try {
                    var irClass = classes.get(ctx.name.getText());
                    currentClass = irClass;

                    //todo: все переменные попадают в текущий scope, это неправильно
                    ctx.defStatement().stream()
                        .map(definition -> (IrVariable) definition.accept(this))
                        .forEach(irClass.fields::add);

                    //todo: надо вызвать findAllFunctions
                    ctx.functionDefinitionStatement().stream()
                        .map(definition -> (IrFunction) definition.accept(this))
                        .forEach(irClass.methods::add);

                    return irClass;
                } finally {
                    currentClass = prev;
                }
            }

            @Override
            public IR visitFunctionDefinitionStatement(IcebergParser.FunctionDefinitionStatementContext ctx) {
                var functionName = currentClass != null
                    ? currentClass.name + "$" + ctx.name.getText()
                    : ctx.name.getText();
                var parametersTypes = ctx.parameters().parameter().stream()
                    .map(parameter -> parameter.type.getText())
                    //TODO: support classes
                    .map(IcebergType::valueOf)
                    .toList();
                var descriptor = new FunctionDescriptor(functionName, parametersTypes);

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

                return new IrStaticCall(
                    functions.get(descriptor),
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
