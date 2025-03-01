package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.target.CompilationUnit;

public interface CompilationPhase {

    void execute(IcebergParser.FileContext file, CompilationUnit unit);
}
