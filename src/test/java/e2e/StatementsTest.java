package e2e;

import iceberg.SemanticException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StatementsTest extends Base {

    @ParameterizedTest
    @MethodSource
    void positive(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> positive() {
        return Stream.of(
            Arguments.of("print 10;", "10\n"),
            Arguments.of("def x = 0; print 10;", "10\n"),
            Arguments.of("if true then print 1;", "1\n"),
            Arguments.of("while false then print 1;", ""),
            Arguments.of("fun foo() {}", ""),
            Arguments.of("class Foo {}", ""),
            Arguments.of("return;", ""),
            Arguments.of("""
                if true then {
                    print 99;
                    return;
                }
                print 100;
                """, "99\n"),
            Arguments.of("def x = 0; x = 10;", ""),
            Arguments.of("""
                fun foo() {}
                foo();""", ""),
            Arguments.of("""
                class X {
                    fun foo() {}
                }
                def x = new X;
                x.foo();
                """, ""),
            Arguments.of("""
                class X {
                    def t: i32
                    fun foo() {}
                }
                def x = new X;
                x.t = 99;
                print x.t;
                """, "99\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void negative(String source, String expected) {
        var exception = assertThrows(SemanticException.class, () -> execute(source, expected));
        assertThat(exception).hasMessageStartingWith("not a statement");
    }

    static Stream<Arguments> negative() {
        return Stream.of(
            Arguments.of("""
                class X {
                    fun foo() {}
                }
                new X;
                """, null),
            Arguments.of("""
                class X {
                    def t: Int
                    fun foo() {}
                }
                def x = new X;
                x.t;
                """, null),
            Arguments.of("not true;", null),
            Arguments.of("-10;", null),
            Arguments.of("2 + 3;", null),
            Arguments.of("2 - 3;", null),
            Arguments.of("2 * 3;", null),
            Arguments.of("2 / 3;", null),
            Arguments.of("2 < 3;", null),
            Arguments.of("2 > 3;", null),
            Arguments.of("2 == 3;", null),
            Arguments.of("2 >= 3;", null),
            Arguments.of("2 <= 3;", null),
            Arguments.of("true and true;", null),
            Arguments.of("false or true;", null),
            Arguments.of("(10);", null),
            Arguments.of("100;", null),
            Arguments.of("\"hello\";", null),
            Arguments.of("true;", null),
            Arguments.of("false;", null),
            Arguments.of("qux;", null),
            Arguments.of("this;", null)
        );
    }
}
