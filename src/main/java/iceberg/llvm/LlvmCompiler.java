package iceberg.llvm;

import iceberg.common.phases.BuildIrTreePhase;
import iceberg.common.phases.DetectInvalidSyntaxPhase;
import iceberg.common.phases.IrVerificationPhase;
import iceberg.fe.CompilationException;
import iceberg.fe.ParsingUtil;
import iceberg.llvm.opt.cp.ConstantPropagation;
import iceberg.llvm.phases.BuildCfgPhase;
import iceberg.llvm.phases.BuildTacPhase;
import iceberg.llvm.phases.CodeGenerationPhase;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.*;

public class LlvmCompiler {

    @SneakyThrows
    public static Path compile(Path sourcePath, String source) {
        var output = compile(source);
        System.out.println(output);

        var sourceName = sourcePath.getFileName().toString().split("\\.ib")[0];

        var llName = sourceName + ".ll";
        var llPath = Paths.get(
            sourcePath.toAbsolutePath().getParent().toString(), llName
        );

        Files.writeString(llPath, output, CREATE, TRUNCATE_EXISTING, WRITE);

        var objectPath = Paths.get(
            sourcePath.toAbsolutePath().getParent().toString(), sourceName
        );

        //TODO: make platform independent
        var process = Runtime.getRuntime().exec(
            "clang %s -o %s -L/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/lib -lc"
                .formatted(llPath, objectPath)
        );

        int exitCode = process.waitFor();
        var out = new String(process.getInputStream().readAllBytes());
        var err = new String(process.getErrorStream().readAllBytes());

        if (err.isBlank() && out.isBlank() && exitCode == 0) {
            return objectPath;
        } else {
            throw new IllegalStateException("""
                Something went wrong
                ### exitCode: %d
                ### out: %s
                ### err: %s
                """.formatted(exitCode, out, err)
            );
        }
    }

    private static String compile(String source) {
        try {
            var file = ParsingUtil.parse(source);

            //compilation process
            new DetectInvalidSyntaxPhase().execute(file);

            var irFile = new BuildIrTreePhase().execute(file);
            new IrVerificationPhase().execute(irFile);

            var allTac = new BuildTacPhase(irFile).execute();
            var allCfg = allTac.stream()
                .map(tacFunction -> new BuildCfgPhase(tacFunction).execute())
                .toList();

            //optimizations
            allCfg.forEach(functionCfg -> new ConstantPropagation(functionCfg).execute());

            return new CodeGenerationPhase(allCfg).execute();
        } catch (CompilationException exception) {
            System.err.println(exception.getMessage());
            throw exception;
        }
    }
}
