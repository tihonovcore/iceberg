package e2e;

import iceberg.CompilationPipeline;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.*;
import static org.assertj.core.api.Assertions.assertThat;

public class Base {

    private static final boolean PRINT_JAVAP = false;
    private static final boolean PRINT_HEX_DUMP = false;

    @SneakyThrows
    void execute(String source, String expected) {
        var bytes = CompilationPipeline.compile(source).iterator().next().bytes;

        var path = Path.of("./src/test/java/e2e/Iceberg.class");
        Files.write(path, bytes, CREATE, WRITE, TRUNCATE_EXISTING);

        if (PRINT_JAVAP) {
            var process = Runtime.getRuntime().exec("javap -c -v ./src/test/java/e2e/Iceberg.class");
            int exitCode = process.waitFor();
            var out = new String(process.getInputStream().readAllBytes());
            System.err.println(out);
            var err = new String(process.getErrorStream().readAllBytes());
            System.err.println(err);
        }

        if (PRINT_HEX_DUMP) {
            var process = Runtime.getRuntime().exec("hexdump -C ./src/test/java/e2e/Iceberg.class");
            int exitCode = process.waitFor();
            var out = new String(process.getInputStream().readAllBytes());
            System.err.println(out);
        }

        var process = Runtime.getRuntime().exec("java -cp ./src/test/java/e2e Iceberg");

        int exitCode = process.waitFor();
        var out = new String(process.getInputStream().readAllBytes());
        var err = new String(process.getErrorStream().readAllBytes());

        assertThat(err).isBlank();
        assertThat(out).isEqualTo(expected);
        assertThat(exitCode).isEqualTo(0);
    }
}
