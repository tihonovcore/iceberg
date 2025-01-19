package e2e;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class LogicExpressionTest extends Base {

    @ParameterizedTest
    @MethodSource
    void logic(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> logic() {
        return Stream.of(
            Arguments.of("print true or true;", "true\n"),
            Arguments.of("print true or false;", "true\n"),
            Arguments.of("print false or true;", "true\n"),
            Arguments.of("print false or false;", "false\n"),

            Arguments.of("print true and true;", "true\n"),
            Arguments.of("print true and false;", "false\n"),
            Arguments.of("print false and true;", "false\n"),
            Arguments.of("print false and false;", "false\n"),

            Arguments.of("print true or true or true;", "true\n"),
            Arguments.of("print true or true or false;", "true\n"),
            Arguments.of("print true or false or true;", "true\n"),
            Arguments.of("print false or true or true;", "true\n"),

            Arguments.of("print true and true and true;", "true\n"),
            Arguments.of("print true and true and false;", "false\n"),
            Arguments.of("print true and false and true;", "false\n"),
            Arguments.of("print false and true and true;", "false\n"),

            Arguments.of("print false or true and true;", "true\n"),
            Arguments.of("print false and true or true;", "true\n"),
            Arguments.of("print false or true and false;", "false\n"),
            Arguments.of("print false and true or false;", "false\n"),

            //TODO: проверять ленивость, например, x != null && x.foo()
            // сейчас мешает семантическая проверка что обе части bool
            // то есть нельзя проверить false or 100 или false or null, нужны переменные

            Arguments.of("print not true;", "false\n"),
            Arguments.of("print not false;", "true\n"),

            Arguments.of("print not (true);", "false\n"),
            Arguments.of("print not (false);", "true\n"),

            Arguments.of("print not (false or true);", "false\n"),
            Arguments.of("print not (true and false);", "true\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void logic_negative(String source) {
        assertThrows(IllegalArgumentException.class, () -> execute(source, null));
    }

    static Stream<Arguments> logic_negative() {
        return Stream.of(
            Arguments.of("print true or 6;"),
            Arguments.of("print false or 6;"),
            Arguments.of("print true and 6;"),
            Arguments.of("print false and 6;"),

            Arguments.of("print 6 or true;"),
            Arguments.of("print 6 or false;"),
            Arguments.of("print 6 and true;"),
            Arguments.of("print 6 and false;"),

            Arguments.of("print true or \"foo\";"),
            Arguments.of("print false or \"foo\";"),
            Arguments.of("print true and \"foo\";"),
            Arguments.of("print false and \"foo\";"),

            Arguments.of("print \"foo\" or true;"),
            Arguments.of("print \"foo\" or false;"),
            Arguments.of("print \"foo\" and true;"),
            Arguments.of("print \"foo\" and false;"),

            //TODO: проверять ленивость, например, x != null && x.foo()
            // сейчас мешает семантическая проверка что обе части bool
            // то есть нельзя проверить false or 100 или false or null, нужны переменные

            Arguments.of("print not 1;"),
            Arguments.of("print not \"foo\";")
        );
    }
}