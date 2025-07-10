package iceberg.jvm.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.ir.*;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

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
                    throw new SemanticException("function already exists");
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
