package e2e;

import iceberg.SemanticException;
import org.junit.jupiter.params.provider.Arguments;
import run.ParameterizedBackendTest;
import run.compiler.Compiler;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static run.BackendTarget.JVM;
import static run.BackendTarget.LLVM;

public class VariablesTest {

    @ParameterizedBackendTest({JVM, LLVM})
    void i32(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
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
                    print x >= y;
                }
                def y = 100;
                print x >= y;
                """, "true\nfalse\n")
        );
    }

    @ParameterizedBackendTest({JVM, LLVM})
    void bool(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
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

    @ParameterizedBackendTest({JVM, LLVM})
    void i64(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
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

    @ParameterizedBackendTest(JVM)
    void string(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
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

    @ParameterizedBackendTest({JVM, LLVM})
    void typedInit(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
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
                def x: i32;
                x = 100;
                print x + 5;""", "105\n"),
            Arguments.of("""
                def x: i64;
                x = 111222333444;
                print x - 2 * 11;""", "111222333422\n"),
            Arguments.of("""
                def x: bool;
                x = false;
                print x or not x;""", "true\n"),

            Arguments.of("""
                def x: i32;
                {
                    x = 100;
                }
                print x + 5;""", "105\n"),
            Arguments.of("""
                def x: i64;
                {
                    x = 111222333444;
                }
                print x - 2 * 11;""", "111222333422\n"),
            Arguments.of("""
                def x: bool;
                {
                    x = false;
                }
                print x or not x;""", "true\n")
        );
    }

    @ParameterizedBackendTest({JVM})
    void typedInit__string(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> typedInit__string() {
        return Stream.of(
            Arguments.of("""
                def x: string = "foo\\nbar\\nqux";
                print x;""", "foo\nbar\nqux\n"),
            Arguments.of("""
                def x: string;
                x = "foo\\nbar\\nqux";
                print x;""", "foo\nbar\nqux\n"),
            Arguments.of("""
                def x: string;
                {
                    x = "foo\\nbar\\nqux";
                }
                print x;""", "foo\nbar\nqux\n")
        );
    }

    @ParameterizedBackendTest({JVM, LLVM})
    void assign(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> assign() {
        return Stream.of(
            Arguments.of("""
                def x = 100;
                print x + 5;
                x = 200;
                print x + 5;""", "105\n205\n"),
            Arguments.of("""
                def x: i64;
                x = 111222333444;
                print x;""", "111222333444\n")
        );
    }

    @ParameterizedBackendTest({JVM, LLVM})
    void negative(Compiler compiler, String source) {
        assertThrows(SemanticException.class, () -> compiler.execute(source, null));
    }

    @SuppressWarnings("unused")
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
                def x: string = 100;"""),
            //todo: too hard to implement?
            //Arguments.of("""
            //    def x: i32;
            //    print x;"""),
            Arguments.of("""
                def x = x + 5;
                print x + 5;"""),
            Arguments.of("""
                def x = 100;
                x = false;"""),
            Arguments.of("""
                def x = 100;
                foo() = false;"""),
            Arguments.of("""
                def x = 100;
                (x) = false;"""),
            Arguments.of("""
                def x = 100;
                49 = false;"""),
            Arguments.of("""
                def x = 100;
                "foo" = false;""")
        );
    }
}
