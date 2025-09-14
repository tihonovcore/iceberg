package e2e;

import iceberg.SemanticException;
import org.junit.jupiter.params.provider.Arguments;
import run.BackendTest;
import run.ParameterizedBackendTest;
import run.compiler.Compiler;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static run.BackendTarget.JVM;

public class FunctionsTest {

    @ParameterizedBackendTest(JVM)
    void noArgs(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
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

    @ParameterizedBackendTest(JVM)
    void args(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
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
                """, "222\n"),
            Arguments.of("""
                fun twice(num: i64): i64 {
                    return num * 2;
                }
                
                print twice(111222333444);
                """, "222444666888\n"),
            Arguments.of("""
                fun add(l: i64, r: i32): i64 {
                    return l + r;
                }
                
                print add(111222333444, -131141);
                """, "111222202303\n"),
            Arguments.of("""
                fun median(a: bool, b: bool, c: bool): bool {
                    return a and b or a and c or b and c;
                }
                
                print median(false, true, false);
                print median(true, false, true);
                """, "false\ntrue\n")
        );
    }

    @BackendTest(JVM)
    void returnFromUnitFunction(Compiler compiler) {
        compiler.execute("""        
            fun foo() {
                print "foo";
                return;
            }
            
            foo();
            """, "foo\n");
    }

    @BackendTest(JVM)
    void returnFromIf(Compiler compiler) {
        compiler.execute("""        
            fun positive(n: i32, fallback: bool): bool {
                if n > 0 then return true;
                else return fallback;
            }

            print positive(-123, false);
            print positive(-123, true);
            """, "false\ntrue\n");
    }

    @BackendTest(JVM)
    void fg(Compiler compiler) {
        compiler.execute("""
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

    @BackendTest(JVM)
    void fac(Compiler compiler) {
        compiler.execute("""
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

    @BackendTest(JVM)
    void fib(Compiler compiler) {
        compiler.execute("""
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

    @BackendTest(JVM)
    void overload(Compiler compiler) {
        compiler.execute("""
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

    @BackendTest(JVM)
    void loop(Compiler compiler) {
        compiler.execute("""
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

    @BackendTest(JVM)
    void unusedReturnValue(Compiler compiler) {
        compiler.execute("""
            fun foo(): i32 {
                return 100;
            }
            
            
            def i = 0;
            while i < 5 then {
                foo(); //returned i32 should be POPed
                i = i + 1;
            }

            print "hello";
            """, "hello\n");
    }

    @BackendTest(JVM)
    void functionInsideFunction(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("""
            fun outer() {
                fun nested() {}
                nested();
            }
            outer();""", null));
        assertThat(exception).hasMessage("function inside function");
    }

    @BackendTest(JVM)
    void redefinition(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("""
            fun foo() {}
            fun foo() {}""", null));
        assertThat(exception).hasMessage("function 'foo' already exists in class Iceberg");
    }

    @BackendTest(JVM)
    void invalidArgsNumber(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("""
            fun foo() {}
            foo(1, 2, 3);""", null));
        assertThat(exception).hasMessage("function 'foo' not found");
    }

    @BackendTest(JVM)
    void invalidArgsTypes(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("""
            fun foo(x: string, i: i32) {}
            foo(98, false);""", null));
        assertThat(exception).hasMessage("function 'foo' not found");
    }


    @BackendTest(JVM)
    void undefinedFunction(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("foo();", null));
        assertThat(exception).hasMessage("function 'foo' not found");
    }

    @BackendTest(JVM)
    void codeAfterReturn(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("""
            fun foo(): i32 {
                return 99;
                print 100;
            }
            """, null));
        assertThat(exception).hasMessage("return statement should be at last position in block");
    }

    @BackendTest(JVM)
    void codeAfterReturn__nested(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("""
            fun foo(): i32 {
                if true then {
                    return 99;
                    print 100;
                }
                return 100;
            }
            """, null));
        assertThat(exception).hasMessage("return statement should be at last position in block");
    }

    @BackendTest(JVM)
    void differentReturnTypes(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("""
            fun foo(): i32 {
                return "string";
            }
            """, null));
        assertThat(exception).hasMessage("""
            bad return in 'foo':
                expected i32
                but was  java/lang/String""");
    }

    @BackendTest(JVM)
    void differentReturnTypes__unitFunction(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("""
            fun foo() {
                return "string";
            }
            """, null));
        assertThat(exception).hasMessage("""
            bad return in 'foo':
                expected unit
                but was  java/lang/String""");
    }

    @BackendTest(JVM)
    void differentReturnTypes__unitReturn(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("""
            fun foo(): string {
                return;
            }
            """, null));
        assertThat(exception).hasMessage("""
            bad return in 'foo':
                expected java/lang/String
                but was  unit""");
    }

    @BackendTest(JVM)
    void tooMuchReturnStatements(Compiler compiler) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute("""
            fun foo() {
                return;
                return;
                return;
            }
            """, null));
        assertThat(exception).hasMessage("too much return statements in block");
    }

    @ParameterizedBackendTest(JVM)
    void explicitReturnWhenFunctionReturnTypeSpecified(Compiler compiler, String source) {
        var exception = assertThrows(SemanticException.class, () -> compiler.execute(source, null));
        assertThat(exception).hasMessage("some branches in 'foo' do not have return statement");
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> explicitReturnWhenFunctionReturnTypeSpecified() {
        return Stream.of(
            Arguments.of("""
                fun foo(): i32 {
                    print 100;
                }
                """),
            Arguments.of("""
                fun foo(): i32 {
                    if true
                    then print 10;
                    else print 99;
                }
                """),
            Arguments.of("""
                fun foo(): i32 {
                    if true
                    then return 10;
                    else print 99;
                }
                """),
            Arguments.of("""
                fun foo(): i32 {
                    if true
                    then print 10;
                    else return 99;
                }
                """),
            Arguments.of("""
                fun foo(): i32 {
                    if true then {
                        print 10;
                    } else {
                        print 99;
                    }
                }
                """),
            Arguments.of("""
                fun foo(): i32 {
                    if true then {
                        print 10;
                    } else {
                        return 99;
                    }
                }
                """),
            Arguments.of("""
                fun foo(): i32 {
                    if true then {
                        return 10;
                    } else {
                        print 99;
                    }
                }
                """)
        );
    }
}
