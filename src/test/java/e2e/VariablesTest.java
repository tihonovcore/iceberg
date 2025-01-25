package e2e;

import iceberg.SemanticException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class VariablesTest extends Base {

    @ParameterizedTest
    @MethodSource
    void i32(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> i32() {
        return Stream.of(
            Arguments.of("""
                def x = 100;
                print x + 2;""", "102\n"),
            Arguments.of("""
                def x = 2 + 2 * 2;
                print x + x * x;""", "42\n"),
            Arguments.of("""
                def x = 2;
                def y = 98;
                print x + y;""", "100\n"),
            Arguments.of("""
                def x = 2;
                def y = 98;
                {
                    print x + y;
                }""", "100\n"),
            Arguments.of("""
                def x = 2;
                {
                    def y = 10;
                    print x * y;
                }
                def y = 100;
                print x * y;
                """, "20\n200\n"),
            Arguments.of("""
                def x = 2;
                {
                    def y = 10;
                    print x < y or x >= y;
                }
                def y = 100;
                print x * y <= -y;
                """, "true\nfalse\n"),
            Arguments.of("""
                def x = 2;
                {
                    def y = x;
                    print x == y;
                }
                def y = 100;
                print x == y;
                """, "true\nfalse\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void bool(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> bool() {
        return Stream.of(
            Arguments.of("""
                def x = false;
                print x or true;""", "true\n"),
            Arguments.of("""
                def x = false or true and true;
                print not x or x and x;""", "true\n"),
            Arguments.of("""
                def x = false;
                def y = true;
                print x or y;""", "true\n"),
            Arguments.of("""
                def x = false;
                def y = true;
                {
                    print x or y;
                }""", "true\n"),
            Arguments.of("""
                def x = true;
                {
                    def y = true;
                    print x and y;
                }
                def y = false;
                print x and y;
                """, "true\nfalse\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void i64(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> i64() {
        return Stream.of(
            Arguments.of("""
                def x = 111222333444;
                print x + 5;""", "111222333449\n"),
            Arguments.of("""
                def x = 111222333444 - 2 * 11;
                print -x + x * 3;""", "222444666844\n"),
            Arguments.of("""
                def x = 111222333444;
                def y = 111222333443;
                print x + y;""", "222444666887\n"),
            Arguments.of("""
                def x = 111222333444;
                def y = 111222333443;
                {
                    print x + y;
                }""", "222444666887\n"),
            Arguments.of("""
                def x = 111222333444;
                {
                    def y = 111222333443;
                    print x + y;
                }
                def y = 2;
                print x + y;
                """, "222444666887\n111222333446\n"),
            Arguments.of("""
                def x = 111222333444;
                {
                    def y = 111222333443;
                    print x < y or x >= y;
                }
                def y = 100;
                print x * y <= -y;
                """, "true\nfalse\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void string(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> string() {
        return Stream.of(
            Arguments.of("""
                def x = "foo";
                print x;""", "foo\n"),
            Arguments.of("""
                def x = "foo\\nbar";
                print x;""", "foo\nbar\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void typedInit(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> typedInit() {
        return Stream.of(
            Arguments.of("""
                def x: i32 = 100;
                print x + 5;""", "105\n"),
            Arguments.of("""
                def x: i64 = 100;
                print x - 1;""", "99\n"),
            Arguments.of("""
                def x: i64 = 111222333444;
                print x - 2 * 11;""", "111222333422\n"),
            Arguments.of("""
                def x: bool = false;
                print x or not x;""", "true\n"),
            Arguments.of("""
                def x: string = "foo\\nbar\\nqux";
                print x;""", "foo\nbar\nqux\n")
            //todo: type and init later
        );
    }

    //todo: assign

    @ParameterizedTest
    @MethodSource
    void negative(String source) {
        assertThrows(SemanticException.class, () -> execute(source, null));
    }

    static Stream<Arguments> negative() {
        return Stream.of(
            Arguments.of("""
                def x = 100;
                def x = 200;"""),
            Arguments.of("""
                def x = 100;
                print y;"""),
            Arguments.of("""
                def x = 100;
                {
                    def x = 200;
                }"""),
            Arguments.of("""
                def x: string = 100;""")
        );
    }
}
