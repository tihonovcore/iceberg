package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.ir.*;

import java.util.Map;
import java.util.stream.Collectors;

public class GenerateMethodsPhase implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        for (var function : unit.irFile.functions) {
            var init = new CompilationUnit.Method();
            init.flags
                = CompilationUnit.Method.AccessFlags.ACC_PUBLIC.value
                | CompilationUnit.Method.AccessFlags.ACC_STATIC.value;

            init.name = unit.constantPool.computeUtf8(function.name);

            //TODO
            if (function.name.equals("main")) {
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

    private CompilationUnit.CodeAttribute createCodeAttribute(
        IrFunction function, CompilationUnit unit
    ) {
        var attribute = new CompilationUnit.CodeAttribute();
        attribute.attributeName = unit.constantPool.computeUtf8("Code");
        attribute.maxStack = 100; //TODO: how to evaluate?
        attribute.maxLocals = 100; //TODO: how to evaluate?

        attribute.function = function;
        attribute.body = function.irBody;

        return attribute;
    }
}
