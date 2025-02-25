package e2e;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class FunctionsTest extends Base {

    @ParameterizedTest
    @MethodSource
    void noArgs(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> noArgs() {
        return Stream.of(
            Arguments.of("""
                fun foo() {
                    print "foo";
                }
                
                foo();
                """, "foo\n"),
            Arguments.of("""
                fun foo(): string {
                    return "foo";
                }
                
                print foo();
                """, "foo\n"),
            Arguments.of("""
                fun foo(): i32 {
                    return 1000;
                }
                
                fun bar(): i32 {
                    return -1;
                }
                
                print foo() + bar();
                """, "999\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void args(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> args() {
        return Stream.of(
            Arguments.of("""
                fun foo(n: i32) {
                    print n;
                }
                
                foo(100);
                """, "100\n"),
            Arguments.of("""
                fun foo(s: string): string {
                    return s;
                }
                
                print foo("hello");
                """, "hello\n"),
            Arguments.of("""
                fun twice(num: i32): i32 {
                    return num * 2;
                }
                
                print twice(111);
                """, "222\n")
        );
    }

    @Test
    void fg() {
        execute("""
            fun f(n: i32): i32 {
                if n == 1 then return 1;

                return n * g(n - 1);
            }
            
            fun g(n: i32): i32 {
                if n == 1 then return 1;

                return 2 * n * f(n - 1);
            }
            
            print f(1);
            print f(2);
            print f(3);
            print f(4);
            """, "1\n2\n12\n48\n");
    }

    @Test
    void fac() {
        execute("""
            fun fac(n: i32): i32 {
                if n == 1 then return 1;

                return n * fac(n - 1);
            }
            
            print fac(1);
            print fac(2);
            print fac(3);
            print fac(4);
            print fac(5);
            """, "1\n2\n6\n24\n120\n");
    }

    @Test
    void fib() {
        execute("""
            fun fib(n: i32): i32 {
                if n == 1 then return 1;
                if n == 2 then return 1;

                return fib(n - 1) + fib(n - 2);
            }
            
            print fib(1);
            print fib(2);
            print fib(3);
            print fib(4);
            print fib(5);
            """, "1\n1\n2\n3\n5\n");
    }

    @Test
    void overload() {
        execute("""
            fun foo() {
                print "foo()";
            }
            
            fun foo(n: i32) {
                print "foo(n)";
            }
            
            fun foo(n: i32, s: string) {
                print "foo(n, s)";
            }
            
            foo();
            foo(1);
            foo(1, "str");
            """, "foo()\nfoo(n)\nfoo(n, s)\n");
    }

    @Test
    void loop() {
        execute("""
            fun foo(n: i32, s: string) {
                def i = 0;
                while i < n then {
                    print s;
                    i = i + 1;
                }
            }
            foo(3, "hello");
            """, "hello\nhello\nhello\n");
    }

    //todo: передать long, bool в параметры
    //todo: вернуть long, bool, null

    //TODO
//    @ParameterizedTest
//    @MethodSource
//    void negative(String source) {
//        assertThrows(SemanticException.class, () -> execute(source, null));
//    }
//
//    static Stream<Arguments> negative() {
//        return Stream.of(
//            Arguments.of("print --100;"),
//            Arguments.of("print -false;"),
//            Arguments.of("print -\"foo\";"),
//            Arguments.of("print false == 100;"),
//            Arguments.of("print false >= 100;"),
//            Arguments.of("print false * true;"),
//            Arguments.of("print false + true;")
//        );
//    }
}
