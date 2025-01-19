package e2e;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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
    void unary_negative(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> unary_negative() {
        return Stream.of(
            Arguments.of("print --100;", "100\n"),
            Arguments.of("print ---100;", "-100\n")
        );
    }
}
