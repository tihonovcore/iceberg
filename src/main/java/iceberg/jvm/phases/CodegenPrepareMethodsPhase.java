package iceberg.jvm.phases;

import iceberg.ir.IrFunction;
import iceberg.jvm.target.CodeAttribute;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.target.Method;

public class CodegenPrepareMethodsPhase {

    public void execute(CompilationUnit unit) {
        var functions = unit.irClass.methods;

        for (var function : functions) {
            var method = new Method();

            method.flags = createFlags(function);
            method.name = unit.constantPool.computeUtf8(function.name);
            method.descriptor = unit.constantPool.computeUtf8(
                function.javaMethodDescriptor()
            );
            method.attributes.add(createCodeAttribute(function, unit));

            unit.methods.add(method);
        }
    }

    private int createFlags(IrFunction irFunction) {
        return "Iceberg".equals(irFunction.irClass.name) && !"<init>".equals(irFunction.name)
            ? Method.AccessFlags.ACC_PUBLIC.value | Method.AccessFlags.ACC_STATIC.value
            : Method.AccessFlags.ACC_PUBLIC.value;
    }

    private CodeAttribute createCodeAttribute(
        IrFunction function, CompilationUnit unit
    ) {
        var attribute = new CodeAttribute();
        attribute.attributeName = unit.constantPool.computeUtf8("Code");
        attribute.maxStack = 100; //TODO: how to evaluate?
        attribute.maxLocals = 100; //TODO: how to evaluate?

        attribute.function = function;

        return attribute;
    }
}
