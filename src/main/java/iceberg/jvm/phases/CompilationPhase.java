package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;

public interface CompilationPhase {

    void execute(IcebergParser.FileContext file, CompilationUnit unit);
}
