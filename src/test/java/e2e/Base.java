package e2e;

import iceberg.CompilationPipeline;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.*;
import static org.assertj.core.api.Assertions.assertThat;

public class Base {

    @SneakyThrows
    void execute(String source, String expected) {
        var bytes = CompilationPipeline.compile(source).iterator().next().bytes;

        var path = Path.of("./src/test/java/e2e/Iceberg.class");
        Files.write(path, bytes, CREATE, WRITE, TRUNCATE_EXISTING);

        var process = Runtime.getRuntime().exec("java -cp ./src/test/java/e2e Iceberg");

        int exitCode = process.waitFor();
        var out = new String(process.getInputStream().readAllBytes());
        var err = new String(process.getErrorStream().readAllBytes());

        try {
            assertThat(err).isBlank();
            assertThat(out).isEqualTo(expected);
            assertThat(exitCode).isEqualTo(0);
        } catch (AssertionError error) {
            printJavap();
            throw error;
        }
    }

    private static void printJavap() throws InterruptedException, IOException {
        var process = Runtime.getRuntime().exec("javap -c -v ./src/test/java/e2e/Iceberg.class");
        process.waitFor();

        var out = new String(process.getInputStream().readAllBytes());
        System.err.println(out);
        var err = new String(process.getErrorStream().readAllBytes());
        System.err.println(err);
    }
}
