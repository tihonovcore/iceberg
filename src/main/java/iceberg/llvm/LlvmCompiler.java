package iceberg.llvm;

import iceberg.common.phases.BuildIrTreePhase;
import iceberg.common.phases.DetectInvalidSyntaxPhase;
import iceberg.common.phases.IrVerificationPhase;
import iceberg.fe.CompilationException;
import iceberg.fe.ParsingUtil;

import java.nio.file.Path;

public class LlvmCompiler {

    public static void compile(Path sourcePath, String source) {
        try {
            var file = ParsingUtil.parse(source);

            //compilation process
            new DetectInvalidSyntaxPhase().execute(file);

            var irFile = new BuildIrTreePhase().execute(file);
            new IrVerificationPhase().execute(irFile);

            var tac = new BuildTacPhase().execute(irFile);
            System.out.println(tac);
        } catch (CompilationException exception) {
            System.err.println(exception.getMessage());
            throw exception;
        }
    }
}
