package iceberg.jvm.phases;

import iceberg.jvm.target.CodeAttribute;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.ir.*;
import iceberg.jvm.target.Method;
import iceberg.jvm.ir.IcebergType;

public class GenerateDefaultConstructor {

    public void execute(CompilationUnit unit) {
        var init = new Method();
        init.flags = Method.AccessFlags.ACC_PUBLIC.value;
        init.name = unit.constantPool.computeUtf8("<init>");
        init.descriptor = unit.constantPool.computeUtf8("()V");
        init.attributes.add(createCodeAttribute(unit));

        unit.methods.add(init);
    }

    private CodeAttribute createCodeAttribute(CompilationUnit unit) {
        var attribute = new CodeAttribute();
        attribute.attributeName = unit.constantPool.computeUtf8("Code");
        attribute.maxStack = 1;
        attribute.maxLocals = 1;

        var objectIrClass = IcebergType.object.irClass;
        var objectConstructor = objectIrClass.methods.stream()
            .filter(fun -> fun.name.equals("<init>"))
            .findFirst().orElseThrow();

        var callSuperStatement = new IrSuperCall(objectConstructor);
        var returnStatement = new IrReturn();

        var function = new IrFunction(IcebergType.string.irClass, "<init>", IcebergType.unit);
        function.irBody.statements.add(callSuperStatement);
        function.irBody.statements.add(returnStatement);

        attribute.function = function;

        return attribute;
    }
}
