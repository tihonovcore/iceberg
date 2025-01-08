package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;

public class GenerateDefaultConstructor implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        var init = new CompilationUnit.Method();
        init.name = unit.constantPool.computeUtf8("<init>");
        init.descriptor = unit.constantPool.computeUtf8("()V");

        unit.methods.add(init);
    }
}
