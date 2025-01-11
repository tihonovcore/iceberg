package e2e;

import iceberg.CompilationPipeline;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.*;
import static org.assertj.core.api.Assertions.assertThat;

public class PrintNumbersTest {

    @ParameterizedTest
    @MethodSource("print")
    void print(String source, String expected) throws IOException, InterruptedException {
        var bytes = CompilationPipeline.compile(source).iterator().next().bytes;

        var path = Path.of("./src/test/java/e2e/Iceberg.class");
        Files.write(path, bytes, CREATE, WRITE, TRUNCATE_EXISTING);

        {
            var process = Runtime.getRuntime().exec("javap -c -v ./src/test/java/e2e/Iceberg.class");
            int exitCode = process.waitFor();
            var out = new String(process.getInputStream().readAllBytes());
            System.err.println(out);
            var err = new String(process.getErrorStream().readAllBytes());
            System.err.println(err);
        }

        {
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

    static Stream<Arguments> print() {
        return Stream.of(
            Arguments.of("print 0;", "0\n"),
            Arguments.of("print 123;", "123\n"),
            Arguments.of("print 500;", "500\n"),
            Arguments.of("print 12345;", "12345\n"),
            Arguments.of("print 111222333;", "111222333\n"),
            Arguments.of("print 2147483647;", "2147483647\n"),
            Arguments.of("print 2147483648;", "2147483648\n"),
            Arguments.of("print 9223372036854775807;", "9223372036854775807\n"),
            Arguments.of("print 9223372036854775808;", "9223372036854775808\n")
        );
    }
}
