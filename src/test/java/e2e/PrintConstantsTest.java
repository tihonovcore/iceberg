package e2e;

import org.junit.jupiter.params.provider.Arguments;
import run.BackendTest;
import run.ParameterizedBackendTest;
import run.compiler.Compiler;

import java.util.stream.Stream;

import static run.BackendTarget.JVM;
import static run.BackendTarget.LLVM;

public class PrintConstantsTest {

    @ParameterizedBackendTest({JVM, LLVM})
    void number(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
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

    @ParameterizedBackendTest({JVM, LLVM})
    void bool(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> bool() {
        return Stream.of(
            Arguments.of("print true;", "true\n"),
            Arguments.of("print false;", "false\n")
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
                print "foo";
                """, "foo\n"),
            Arguments.of("""
                print "бар";
                """, "бар\n"),
            Arguments.of("""
                print "Ǡ Ѩ ɚ ȡ";
                """, "Ǡ Ѩ ɚ ȡ\n"),
            Arguments.of("""
                print "12345";
                """, "12345\n"),
            Arguments.of("""
                print "\\"foo";
                """, "\"foo\n"),
            Arguments.of("""
                print "\\"foo\\"";
                """, "\"foo\"\n"),
            Arguments.of("""
                print "foo\\"";
                """, "foo\"\n"),
            Arguments.of("""
                print "foo\\"bar";
                """, "foo\"bar\n"),
            Arguments.of("""
                print "\\nfoo";
                """, "\nfoo\n"),
            Arguments.of("""
                print "\\nfoo\\n";
                """, "\nfoo\n\n"),
            Arguments.of(
                """
                print "foo\\n";
                """, "foo\n\n"),
            Arguments.of("""
                print "foo\\nbar";
                """, "foo\nbar\n")
        );
    }

    @BackendTest(JVM)
    void multiline(Compiler compiler) {
        compiler.execute("""
            print 123;
            print -123;
            print false;
            print "from \\"рога и копыта\\" inc";
            print "on top\\non bottom";
            print 123456789;
            print 12345678987654321;
            """, """
            123
            -123
            false
            from "рога и копыта" inc
            on top
            on bottom
            123456789
            12345678987654321
            """);
    }
}
