package e2e;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class EqualityExpressionTest extends Base {

    @ParameterizedTest
    @MethodSource
    void equals(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> equals() {
        return Stream.of(
            Arguments.of("print 100 == 101;", "false\n"),
            Arguments.of("print 100 == 100;", "true\n"),
            Arguments.of("print 100 == 99;", "false\n"),
            Arguments.of("print -100 == 100;", "false\n"),
            Arguments.of("print -100 == 0;", "false\n"),
            Arguments.of("print -100 == -99;", "false\n"),
            Arguments.of("print -100 == -100;", "true\n"),
            Arguments.of("print -100 == -101;", "false\n"),
            Arguments.of("print 2 + 2 == 5;", "false\n"),
            Arguments.of("print 2 * 2 + 1 == 1 * 2 + 3;", "true\n"),
            Arguments.of("print 2 == 2 + 1 and 2 + 2 == 5;", "false\n"),
            Arguments.of("print 0 == 111222333445;", "false\n"),
            Arguments.of("print 111222333444 == 0;", "false\n"),
            Arguments.of("print 111222333444 == 111222333445;", "false\n"),
            Arguments.of("print 111222333445 == 111222333445;", "true\n"),
            Arguments.of("print true == true;", "true\n"),
            Arguments.of("print true == false;", "false\n"),
            Arguments.of("print true == true and true;", "true\n"),
            Arguments.of("print true == true and false;", "false\n"),
            Arguments.of("print true or true == false;", "true\n"),
            Arguments.of("print false or true == false;", "false\n"),
            Arguments.of("print false or true == true;", "true\n"),
            Arguments.of("print \"foo\" == \"foo\";", "true\n"),
            Arguments.of("print \"foo\" == \"bar\";", "false\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void notEquals(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> notEquals() {
        return Stream.of(
            Arguments.of("print 100 != 101;", "true\n"),
            Arguments.of("print 100 != 100;", "false\n"),
            Arguments.of("print 100 != 99;", "true\n"),
            Arguments.of("print -100 != 100;", "true\n"),
            Arguments.of("print -100 != 0;", "true\n"),
            Arguments.of("print -100 != -99;", "true\n"),
            Arguments.of("print -100 != -100;", "false\n"),
            Arguments.of("print -100 != -101;", "true\n"),
            Arguments.of("print 2 + 2 != 5;", "true\n"),
            Arguments.of("print 2 * 2 + 1 != 1 * 2 + 3;", "false\n"),
            Arguments.of("print 2 != 2 + 1 and 2 + 2 != 5;", "true\n"),
            Arguments.of("print 0 != 111222333445;", "true\n"),
            Arguments.of("print 111222333444 != 0;", "true\n"),
            Arguments.of("print 111222333444 != 111222333445;", "true\n"),
            Arguments.of("print 111222333445 != 111222333445;", "false\n"),
            Arguments.of("print true != true;", "false\n"),
            Arguments.of("print true != false;", "true\n"),
            Arguments.of("print true != true and true;", "false\n"),
            Arguments.of("print true != true and false;", "false\n"),
            Arguments.of("print true or true != false;", "true\n"),
            Arguments.of("print false or true != false;", "true\n"),
            Arguments.of("print false or true != true;", "false\n"),
            Arguments.of("print \"foo\" != \"foo\";", "false\n"),
            Arguments.of("print \"foo\" != \"bar\";", "true\n")
        );
    }
}
