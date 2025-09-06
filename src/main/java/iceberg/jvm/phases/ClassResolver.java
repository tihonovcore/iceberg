package iceberg.jvm.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.ir.*;
import lombok.Getter;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassResolver {

    @Getter
    private final IrClass icebergIrClass = new IrClass("Iceberg");
    private final Map<String, IrClass> allClasses = new HashMap<>();

    public ClassResolver(IcebergParser.FileContext file) {
        findAllClasses(file);
        findAllFunctions(file);
    }

    public IrClass getIrClass(String name) {
        return allClasses.get(name);
    }

    private void findAllClasses(IcebergParser.FileContext file) {
        allClasses.put(icebergIrClass.name, icebergIrClass);

        file.accept(new IcebergBaseVisitor<>() {
            @Override
            public Object visitDependency(IcebergParser.DependencyContext ctx) {
                var fqn = ctx.ID().stream()
                    .map(ParseTree::getText)
                    .collect(Collectors.joining("."));

                Class<?> klass;
                try {
                    klass = Class.forName(fqn);
                } catch (ClassNotFoundException e) {
                    throw new SemanticException("unknown class: " + fqn);
                }

                importJavaClass(klass);

                return super.visitDependency(ctx);
            }

            @Override
            public Object visitClassDefinitionStatement(IcebergParser.ClassDefinitionStatementContext ctx) {
                var name = ctx.name.getText();
                if (allClasses.containsKey(name)) {
                    throw new SemanticException("class already exists");
                }

                allClasses.put(name, new IrClass(name));
                return super.visitClassDefinitionStatement(ctx);
            }
        });
    }

    private IrClass importJavaClass(Class<?> klass) {
        if (allClasses.containsKey(klass.getSimpleName())) {
            return allClasses.get(klass.getSimpleName());
        }

        var fqn = klass.getCanonicalName().replace('.', '/');
        var irClass = new IrClass(fqn);
        allClasses.put(klass.getSimpleName(), irClass);

        for (var javaMethod : klass.getMethods()) {
            var returnType = buildType(javaMethod.getReturnType());
            var irFunction = new IrFunction(irClass, javaMethod.getName(), returnType);

            for (var javaParam : javaMethod.getParameters()) {
                var paramType = buildType(javaParam.getType());
                var irVariable = new IrVariable(paramType, null);
                irFunction.parameters.add(irVariable);
            }

            irClass.methods.add(irFunction);
        }

        return irClass;
    }

    private IcebergType buildType(Class<?> klass) {
        var builtInType = IcebergType.valueOf(klass);
        if (builtInType != null) {
            return builtInType;
        }

        //TODO: перенести создание типа в IrClass?
        return new IcebergType(importJavaClass(klass));
    }

    private void findAllFunctions(IcebergParser.FileContext file) {
        file.accept(new IcebergBaseVisitor<Void>() {

            IrClass currentClass = icebergIrClass;

            @Override
            public Void visitClassDefinitionStatement(IcebergParser.ClassDefinitionStatementContext ctx) {
                var prev = currentClass;
                try {
                    currentClass = allClasses.get(ctx.name.getText());
                    return super.visitClassDefinitionStatement(ctx);
                } finally {
                    currentClass = prev;
                }
            }

            @Override
            public Void visitFunctionDefinitionStatement(
                IcebergParser.FunctionDefinitionStatementContext ctx
            ) {
                var functionName = ctx.name.getText();
                var parametersTypes = ctx.parameters().parameter().stream()
                    .map(parameter -> parameter.type.getText())
                    //TODO: support user-defined types
                    .map(IcebergType::valueOf)
                    .toList();

                var optional = currentClass.findMethod(functionName, parametersTypes);
                if (optional.isPresent()) {
                    throw new SemanticException(
                        "function '%s' already exists".formatted(functionName)
                    );
                }

                IcebergType returnType;
                if (ctx.returnType == null) {
                    returnType = IcebergType.unit;
                } else if (allClasses.containsKey(ctx.returnType.getText())) {
                    var irClass = allClasses.get(ctx.returnType.getText());
                    returnType = new IcebergType(irClass);
                } else {
                    returnType = IcebergType.valueOf(ctx.returnType.getText());
                }
                var function = new IrFunction(currentClass, functionName, returnType);

                ctx.parameters().parameter().stream()
                    .map(parameter -> IcebergType.valueOf(parameter.type.getText()))
                    .map(parameter -> new IrVariable(parameter, null))
                    .forEach(function.parameters::add);

                currentClass.methods.add(function);

                return null;
            }
        });
    }
}
