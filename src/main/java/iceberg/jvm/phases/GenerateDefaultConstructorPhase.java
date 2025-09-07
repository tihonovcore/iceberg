package iceberg.jvm.phases;

import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.ir.*;
import iceberg.jvm.ir.IcebergType;

import java.util.Collection;

public class GenerateDefaultConstructorPhase {

    public void execute(CompilationUnit unit) {
        var objectIrClass = IcebergType.object.irClass;
        var objectConstructor = objectIrClass.methods.stream()
            .filter(fun -> fun.name.equals("<init>"))
            .findFirst().orElseThrow();

        var callSuperStatement = new IrSuperCall(objectConstructor);
        var fieldInitializers = buildFieldInitializers(unit);
        var returnStatement = new IrReturn();

        var function = new IrFunction(unit.irClass, "<init>", IcebergType.unit);
        function.irBody.statements.add(callSuperStatement);
        function.irBody.statements.addAll(fieldInitializers);
        function.irBody.statements.add(returnStatement);

        unit.irClass.methods.add(function);
    }

    private Collection<IrPutField> buildFieldInitializers(CompilationUnit unit) {
        var irClass = unit.irClass;
        return irClass.fields.values().stream()
            .filter(irField -> irField.initializer != null)
            .map(irField -> {
                var receiver = new IrThis(irClass);
                return new IrPutField(receiver, irField, irField.initializer);
            })
            .toList();
    }
}
