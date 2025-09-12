package run.compiler;

import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.*;
import static org.assertj.core.api.Assertions.assertThat;

public class JvmCompiler extends Compiler {
    @Override
    @SneakyThrows
    protected void execute(File workDirectory, String source, String expected) {
        for (var unit : iceberg.jvm.JvmCompiler.compile(source)) {
            var className = unit.irClass == null ? "Iceberg" : unit.irClass.name;

            var path = Path.of(workDirectory.getAbsolutePath(), className + ".class");
            java.nio.file.Files.write(path, unit.bytes, CREATE, WRITE, TRUNCATE_EXISTING);
        }

        var process = Runtime.getRuntime().exec("java -cp %s Iceberg".formatted(workDirectory.getAbsolutePath()));

        int exitCode = process.waitFor();
        var out = new String(process.getInputStream().readAllBytes());
        var err = new String(process.getErrorStream().readAllBytes());

        try {
            assertThat(err).isBlank();
            assertThat(out).isEqualTo(expected);
            assertThat(exitCode).isEqualTo(0);
        } catch (AssertionError error) {
            printJavap(workDirectory);
            throw error;
        }
    }

    private void printJavap(File workDirectory) throws InterruptedException, IOException {
        var icebergClass = Path.of(workDirectory.getAbsolutePath(), "Iceberg.class");
        var process = Runtime.getRuntime().exec("javap -c -v %s".formatted(icebergClass));
        process.waitFor();

        var out = new String(process.getInputStream().readAllBytes());
        System.err.println(out);
        var err = new String(process.getErrorStream().readAllBytes());
        System.err.println(err);
    }
}
