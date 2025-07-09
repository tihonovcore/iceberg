package iceberg.jvm.phases;

import iceberg.jvm.target.CodeAttribute;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.ir.*;
import iceberg.jvm.target.Method;

public class GenerateMethodsPhase {

    public void execute(CompilationUnit unit) {
        var functions = unit.irClass.methods;

        for (var function : functions) {
            var init = new Method();
            //TODO: методы класса должны быть не статическими
            init.flags
                = Method.AccessFlags.ACC_PUBLIC.value
                | Method.AccessFlags.ACC_STATIC.value;

            init.name = unit.constantPool.computeUtf8(function.name);

            if (function.name.equals("main")) {
                //TODO: create separate IcebergType?
                init.descriptor = unit.constantPool.computeUtf8("([Ljava/lang/String;)V");
            } else {
                init.descriptor = unit.constantPool.computeUtf8(
                    function.javaMethodDescriptor()
                );
            }

            init.attributes.add(createCodeAttribute(function, unit));

            unit.methods.add(init);
        }
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
