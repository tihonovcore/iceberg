package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.ir.IrBody;
import iceberg.jvm.ir.IrSuperCall;
import iceberg.jvm.ir.IrReturn;

public class GenerateDefaultConstructor implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        var init = new CompilationUnit.Method();
        init.flags = CompilationUnit.Method.AccessFlags.ACC_PUBLIC.value;
        init.name = unit.constantPool.computeUtf8("<init>");
        init.descriptor = unit.constantPool.computeUtf8("()V");
        init.attributes.add(createCodeAttribute(unit));

        unit.methods.add(init);
    }

    private CompilationUnit.CodeAttribute createCodeAttribute(CompilationUnit unit) {
        var attribute = new CompilationUnit.CodeAttribute();
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

        var body = new IrBody();
        body.statements.add(callSuperStatement);
        body.statements.add(returnStatement);
        attribute.body = body;

        return attribute;
    }
}
