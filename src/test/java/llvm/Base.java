package llvm;

import iceberg.llvm.LlvmCompiler;
import lombok.SneakyThrows;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Base {

    @TempDir
    File workDirectory;

    @SneakyThrows
    void execute(String source, String expected) {
        var fakeSourcePath = Path.of(workDirectory.getAbsolutePath(), "Iceberg.ib");
        var objectPath = LlvmCompiler.compile(fakeSourcePath, source);

        var process = Runtime.getRuntime().exec(objectPath.toAbsolutePath().toString());

        int exitCode = process.waitFor();
        var out = new String(process.getInputStream().readAllBytes());
        var err = new String(process.getErrorStream().readAllBytes());

        assertThat(err).isBlank();
        assertThat(out).isEqualTo(expected);
        assertThat(exitCode).isEqualTo(0);
    }
}
