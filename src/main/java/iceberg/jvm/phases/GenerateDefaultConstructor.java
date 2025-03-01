package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.target.CodeAttribute;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.ir.*;
import iceberg.jvm.target.Method;
import iceberg.jvm.ir.IcebergType;

public class GenerateDefaultConstructor implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
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

        var constructor = unit.constantPool.computeNameAndType(
            unit.constantPool.computeUtf8("<init>"),
            unit.constantPool.computeUtf8("()V")
        );
        var methodRef = unit.constantPool.computeMethodRef(unit.constantPool.OBJECT, constructor);

        var callSuperStatement = new IrSuperCall(methodRef);
        var returnStatement = new IrReturn();

        var function = new IrFunction(IcebergType.string.irClass, "<init>", IcebergType.unit);
        function.irBody.statements.add(callSuperStatement);
        function.irBody.statements.add(returnStatement);

        attribute.function = function;

        return attribute;
    }
}
