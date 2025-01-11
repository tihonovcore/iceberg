package e2e;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class PrintConstantsTest extends Base {

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

    @ParameterizedTest
    @MethodSource
    void bool(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> bool() {
        return Stream.of(
            Arguments.of("print true;", "true\n"),
            Arguments.of("print false;", "false\n")
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

    @Test
    void multiline() {
        execute("""
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
