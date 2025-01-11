package e2e;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class PrintNumbersTest extends Base {

    @ParameterizedTest
    @MethodSource
    void number(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> number() {
        return Stream.of(
            Arguments.of("print 0;", "0\n"),
            Arguments.of("print 123;", "123\n"),
            Arguments.of("print 500;", "500\n"),
            Arguments.of("print 12345;", "12345\n"),
            Arguments.of("print 111222333;", "111222333\n"),
            Arguments.of("print 2147483647;", "2147483647\n"),
            Arguments.of("print 2147483648;", "2147483648\n"),
            Arguments.of("print 9223372036854775807;", "9223372036854775807\n"),

            Arguments.of("print -1;", "-1\n"),
            Arguments.of("print -123;", "-123\n"),
            Arguments.of("print -500;", "-500\n"),
            Arguments.of("print -12345;", "-12345\n"),
            Arguments.of("print -111222333;", "-111222333\n"),
            Arguments.of("print -2147483647;", "-2147483647\n"),
            Arguments.of("print -2147483648;", "-2147483648\n"),
            Arguments.of("print -9223372036854775808;", "-9223372036854775808\n")
        );
    }
}
