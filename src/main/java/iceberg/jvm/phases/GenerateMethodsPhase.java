package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.target.CodeAttribute;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.ir.*;
import iceberg.jvm.target.Method;
import iceberg.jvm.ir.IcebergType;

import java.util.Map;
import java.util.stream.Collectors;

public class GenerateMethodsPhase implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        //TODO: уйти от irFile к IrClass
        var functions = unit.irFile != null
            ? unit.irFile.functions
            : unit.irClass.methods;

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
                var mapping = Map.of(
                    IcebergType.i32, "I",
                    IcebergType.i64, "J",
                    IcebergType.bool, "Z",
                    IcebergType.string, "Ljava/lang/String;",
                    IcebergType.unit, "V"
                );

                var params = function.parameters.stream()
                    .map(v -> v.type)
                    .map(mapping::get)
                    .collect(Collectors.joining(""));

                init.descriptor = unit.constantPool.computeUtf8(
                    "(" + params + ")" + mapping.get(function.returnType)
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
