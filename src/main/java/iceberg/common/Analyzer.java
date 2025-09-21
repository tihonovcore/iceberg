package iceberg.common;

import iceberg.CompilationException;
import iceberg.SemanticException;
import iceberg.common.phases.BuildIrTreePhase;
import iceberg.common.phases.DetectInvalidSyntaxPhase;
import iceberg.common.phases.IrVerificationPhase;
import iceberg.common.phases.ParseSourcePhase;

public class Analyzer {

    public void analyze(String source) throws CompilationException, SemanticException {
        var astFile = new ParseSourcePhase().execute(source);
        new DetectInvalidSyntaxPhase().execute(astFile);

        var irFile = new BuildIrTreePhase().execute(astFile);
        new IrVerificationPhase().execute(irFile);
    }
}
