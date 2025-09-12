package run.compiler;

import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class LlvmCompiler extends Compiler {
    @Override
    @SneakyThrows
    protected void execute(File workDirectory, String source, String expectedOutput) {
        var fakeSourcePath = Path.of(workDirectory.getAbsolutePath(), "Iceberg.ib");
        var objectPath = iceberg.llvm.LlvmCompiler.compile(fakeSourcePath, source);

        var process = Runtime.getRuntime().exec(objectPath.toAbsolutePath().toString());

        int exitCode = process.waitFor();
        var out = new String(process.getInputStream().readAllBytes());
        var err = new String(process.getErrorStream().readAllBytes());

        assertThat(err).isBlank();
        assertThat(out).isEqualTo(expectedOutput);
        assertThat(exitCode).isEqualTo(0);
    }
}
