package e2e;

import iceberg.fe.CompilationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArithmeticExpressionTest extends Base {

    @ParameterizedTest
    @MethodSource
    void unary(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> unary() {
        return Stream.of(
            Arguments.of("print -(100);", "-100\n"),
            Arguments.of("print -0;", "0\n"),
            Arguments.of("print -(0);", "0\n"),
            Arguments.of("print -(12345);", "-12345\n"),
            Arguments.of("print -(111222333444);", "-111222333444\n"),
            Arguments.of("print -(2147483647);", "-2147483647\n"),
            Arguments.of("print -(2147483648);", "-2147483648\n"),
            Arguments.of("print -(111222333444);", "-111222333444\n"),
            Arguments.of("print -(9223372036854775807);", "-9223372036854775807\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void unary_negative(String source) {
        assertThrows(CompilationException.class, () -> execute(source, null));
    }

    static Stream<Arguments> unary_negative() {
        return Stream.of(
            Arguments.of("print --100;"),
            Arguments.of("print ---100;")
        );
    }

    @ParameterizedTest
    @MethodSource
    void addition(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> addition() {
        return Stream.of(
            Arguments.of("print 2 + 4;", "6\n"),
            Arguments.of("print 2 + 2;", "4\n"),
            Arguments.of("print 2 + 0;", "2\n"),
            Arguments.of("print 2 + -2;", "0\n"),
            Arguments.of("print 2 + -4;", "-2\n"),
            Arguments.of("print 111 + 222 + 333;", "666\n"),
            Arguments.of("print -111 + -222 + -333;", "-666\n"),
            Arguments.of("print 1 + -2 + 3 + -4 + 5 + -6;", "-3\n"),

            Arguments.of("print 111222333440 + 4;", "111222333444\n"),
            Arguments.of("print 4 + 111222333440;", "111222333444\n"),
            Arguments.of("print 111222333444 + 111222333444;", "222444666888\n")
        );
    }
}
