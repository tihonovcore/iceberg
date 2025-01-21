package e2e;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class ComparationExpressionTest extends Base {

    @ParameterizedTest
    @MethodSource
    void lt(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> lt() {
        return Stream.of(
            Arguments.of("print 100 < 101;", "true\n"),
            Arguments.of("print 100 < 100;", "false\n"),
            Arguments.of("print 100 < 99;", "false\n"),
            Arguments.of("print -100 < 100;", "true\n"),
            Arguments.of("print -100 < 0;", "true\n"),
            Arguments.of("print -100 < -99;", "true\n"),
            Arguments.of("print -100 < -100;", "false\n"),
            Arguments.of("print -100 < -101;", "false\n"),
            Arguments.of("print 2 + 2 < 5;", "true\n"),
            Arguments.of("print 2 + 2 < 1 * 2 + 3;", "true\n"),
            Arguments.of("print 2 < 2 + 1 and 2 + 2 < 5;", "true\n"),
            Arguments.of("print 0 < 111222333445;", "true\n"),
            Arguments.of("print 111222333444 < 0;", "false\n"),
            Arguments.of("print 111222333444 < 111222333445;", "true\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void le(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> le() {
        return Stream.of(
            Arguments.of("print 100 <= 101;", "true\n"),
            Arguments.of("print 100 <= 100;", "true\n"),
            Arguments.of("print 100 <= 99;", "false\n"),
            Arguments.of("print -100 <= 100;", "true\n"),
            Arguments.of("print -100 <= 0;", "true\n"),
            Arguments.of("print -100 <= -99;", "true\n"),
            Arguments.of("print -100 <= -100;", "true\n"),
            Arguments.of("print -100 <= -101;", "false\n"),
            Arguments.of("print 2 + 2 <= 5;", "true\n"),
            Arguments.of("print 2 + 2 <= 1 * 2 + 3;", "true\n"),
            Arguments.of("print 2 <= 2 + 1 and 2 + 2 <= 5;", "true\n"),
            Arguments.of("print 0 <= 111222333445;", "true\n"),
            Arguments.of("print 111222333444 <= 0;", "false\n"),
            Arguments.of("print 111222333444 <= 111222333445;", "true\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void gt(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> gt() {
        return Stream.of(
            Arguments.of("print 100 > 101;", "false\n"),
            Arguments.of("print 100 > 100;", "false\n"),
            Arguments.of("print 100 > 99;", "true\n"),
            Arguments.of("print -100 > 100;", "false\n"),
            Arguments.of("print -100 > 0;", "false\n"),
            Arguments.of("print -100 > -99;", "false\n"),
            Arguments.of("print -100 > -100;", "false\n"),
            Arguments.of("print -100 > -101;", "true\n"),
            Arguments.of("print 2 + 2 > 3;", "true\n"),
            Arguments.of("print 2 + 2 > 1 * 2 + 3;", "false\n"),
            Arguments.of("print 2 > 2 + 1 and 2 + 2 > 5;", "false\n"),
            Arguments.of("print 0 > 111222333445;", "false\n"),
            Arguments.of("print 111222333444 > 0;", "true\n"),
            Arguments.of("print 111222333444 > 111222333445;", "false\n"),
            Arguments.of("print 111222333445 > 111222333445;", "false\n"),
            Arguments.of("print 111222333445 > 111222333444;", "true\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void ge(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> ge() {
        return Stream.of(
            Arguments.of("print 100 >= 101;", "false\n"),
            Arguments.of("print 100 >= 100;", "true\n"),
            Arguments.of("print 100 >= 99;", "true\n"),
            Arguments.of("print -100 >= 100;", "false\n"),
            Arguments.of("print -100 >= 0;", "false\n"),
            Arguments.of("print -100 >= -99;", "false\n"),
            Arguments.of("print -100 >= -100;", "true\n"),
            Arguments.of("print -100 >= -101;", "true\n"),
            Arguments.of("print 2 + 2 >= 5;", "false\n"),
            Arguments.of("print 2 + 2 >= 1 * 2 + 3;", "false\n"),
            Arguments.of("print 3 >= 2 + 1 and 2 + 2 >= 4;", "true\n"),
            Arguments.of("print 0 >= 111222333445;", "false\n"),
            Arguments.of("print 111222333444 >= 0;", "true\n"),
            Arguments.of("print 111222333444 >= 111222333445;", "false\n"),
            Arguments.of("print 111222333444 >= 111222333444;", "true\n")
        );
    }
}
