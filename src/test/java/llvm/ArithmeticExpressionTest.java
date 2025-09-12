package llvm;

import iceberg.SemanticException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

//TODO: многие тесты можно запускать и в JVM и в LLVM
// придумать как это сделать не дублируя тесты (мб свои аннотации
// @BackendTest({JVM, LLVM}) на замену @Test и @ParametrizedTest
public class ArithmeticExpressionTest extends Base {

    @Disabled //TODO
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

    @ParameterizedTest
    @MethodSource
    void subtraction(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> subtraction() {
        return Stream.of(
            Arguments.of("print 2 - 4;", "-2\n"),
            Arguments.of("print 2 - 2;", "0\n"),
            Arguments.of("print 2 - 0;", "2\n"),
            Arguments.of("print 2 - -2;", "4\n"),
            Arguments.of("print 2 - -4;", "6\n"),
            Arguments.of("print 111 - 222 - 333;", "-444\n"),
            Arguments.of("print -111 - -222 - -333;", "444\n"),
            Arguments.of("print 1 - -2 - 3 - -4 - 5 - -6;", "5\n"),

            Arguments.of("print 111222333448 - 4;", "111222333444\n"),
            Arguments.of("print 4 - 111222333448;", "-111222333444\n"),
            Arguments.of("print 111222333444 - 111222333444;", "0\n"),

            Arguments.of("print -111222333440 - 4;", "-111222333444\n"),
            Arguments.of("print -4 - 111222333440;", "-111222333444\n"),
            Arguments.of("print -111222333444 - 111222333444;", "-222444666888\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void multiplication(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> multiplication() {
        return Stream.of(
            Arguments.of("print 2 * 4;", "8\n"),
            Arguments.of("print 2 * 2;", "4\n"),
            Arguments.of("print 2 * 0;", "0\n"),
            Arguments.of("print 2 * -2;", "-4\n"),
            Arguments.of("print 2 * -4;", "-8\n"),
            Arguments.of("print 111 * 222 * 333;", "8205786\n"),
            Arguments.of("print -111 * -222* -333;", "-8205786\n"),
            Arguments.of("print 1 * -2 * 3 * -4 * 5 * -6;", "-720\n"),

            Arguments.of("print 111222333444 * 2;", "222444666888\n"),
            Arguments.of("print 2 * 111222333444;", "222444666888\n"),

            Arguments.of("print -111222333444 * 2;", "-222444666888\n"),
            Arguments.of("print 111222333444 * -2;", "-222444666888\n"),
            Arguments.of("print -111222333444 * -2;", "222444666888\n"),
            Arguments.of("print -2 * 111222333444;", "-222444666888\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void division(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> division() {
        return Stream.of(
            Arguments.of("print 2 / 4;", "0\n"),
            Arguments.of("print 2 / 2;", "1\n"),
            Arguments.of("print 2 / -2;", "-1\n"),
            Arguments.of("print 2 / -4;", "0\n"),
            Arguments.of("print 4 / 2;", "2\n"),
            Arguments.of("print 5 / 2;", "2\n"),
            Arguments.of("print -4 / 2;", "-2\n"),
            Arguments.of("print -5 / 2;", "-2\n"),
            Arguments.of("print 8205786 / 333 / 222;", "111\n"),
            Arguments.of("print -8205786 / -333 / -222;", "-111\n"),
            Arguments.of("print -720 / -2 / 3 / -4 / 5 / -6;", "1\n"),

            Arguments.of("print 222444666888 / 2;", "111222333444\n"),
            Arguments.of("print 2 / 111222333444;", "0\n"),

            Arguments.of("print 222444666888 / 222444666888;", "1\n"),
            Arguments.of("print -222444666888 / 222444666888;", "-1\n"),

            Arguments.of("print -222444666888 / 2;", "-111222333444\n"),
            Arguments.of("print 222444666888 / -2;", "-111222333444\n"),
            Arguments.of("print -222444666888 / -2;", "111222333444\n"),
            Arguments.of("print -2 / 111222333444;", "0\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void complex(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> complex() {
        return Stream.of(
            Arguments.of("print 2 + 2 * 2;", "6\n"),
            Arguments.of("print 2 - 2 * 2;", "-2\n"),
            Arguments.of("print 2 + 2 / 2;", "3\n"),
            Arguments.of("print 2 - 2 / 2;", "1\n"),

            Arguments.of("print (2 + 2) * 2;", "8\n"),
            Arguments.of("print (2 - 2) * 2;", "0\n"),
            Arguments.of("print (2 + 2) / 2;", "2\n"),
            Arguments.of("print (2 - 2) / 2;", "0\n"),

            Arguments.of("print 5 * (300 - 200) / 2 + 4;", "254\n"),

            Arguments.of("print 2 + 111222333443 * 2;", "222444666888\n"),
            Arguments.of("print 222444666888 - 111222333443 * 2;", "2\n"),
            Arguments.of("print 111222333444 + 222444666888 / 111222333444;", "111222333446\n"),
            Arguments.of("print 111222333444 - 111222333444 / 111222333444;", "111222333443\n"),

            Arguments.of("print (111222333444 + 2) * 2;", "222444666892\n"),
            Arguments.of("print (111222333444 - 222444666888) * 2;", "-222444666888\n"),
            Arguments.of("print (111222333444 + 111222333444) / 2;", "111222333444\n"),
            Arguments.of("print (111222333444 - 222444666888) / 111222333444;", "-1\n"),

            Arguments.of("print 5 * (300 - 200) / 2 + 4;", "254\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void negative(String source) {
        assertThrows(SemanticException.class, () -> execute(source, null));
    }

    static Stream<Arguments> negative() {
        return Stream.of(
            Arguments.of("print --100;"),
            Arguments.of("print -false;"),
            Arguments.of("print -\"foo\";"),
            Arguments.of("print false == 100;"),
            Arguments.of("print false >= 100;"),
            Arguments.of("print false * true;"),
            Arguments.of("print false + true;"),
            Arguments.of("123;"),
            Arguments.of("2 + 2 * 2;")
        );
    }
}
