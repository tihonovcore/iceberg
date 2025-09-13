package e2e;

import org.junit.jupiter.params.provider.Arguments;
import run.BackendTest;
import run.ParameterizedBackendTest;
import run.compiler.Compiler;

import java.util.stream.Stream;

import static run.BackendTarget.JVM;
import static run.BackendTarget.LLVM;

public class LoopStatementTest {

    @ParameterizedBackendTest({JVM, LLVM})
    void loop(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> loop() {
        return Stream.of(
            Arguments.of("""
                def i = 0;
                while i < 5 then {
                    print i;
                    i = i + 1;
                }""", "0\n1\n2\n3\n4\n"),
            Arguments.of("""
                def a = 2147483646;
                def b = 123456;
                def tmp = 0;
                while b != 0 then {
                    tmp = b;
                    b = a - (a / b) * b;
                    a = tmp;
                }
                
                print a + b;
                """, "6\n"),
            Arguments.of("""
                def a = 2147483646;
                def b = 123456;
                while b != 0 then {
                    def tmp = b;
                    b = a - (a / b) * b;
                    a = tmp;
                }
                
                print a + b;
                """, "6\n"),
            Arguments.of("""
                //numbers itself are i32, so we have to split expression to prevent overflow
                def a: i64 = 2 * 3 * 5 * 7 * 11 * 13 * 17 * 19;
                a = a * 23 * 29 * 31 * 37 * 39 * 41 * 43;
                
                def b: i64 = 31 * 41;
                
                while b != 0 then {
                    def tmp: i64 = b;
                    b = a - (a / b) * b;
                    a = tmp;
                }
                
                print a + b;
                """, "1271\n"),
            Arguments.of("""
                def x = 0;
                
                def i = 0;
                while i < 1000 then {
                    def j = 0;
                    while j < 1000 then {
                        x = x + 11;
                        j = j + 1;
                    }
                    i = i + 1;
                }
                
                print x;
                """, "11000000\n"),
            Arguments.of("""
                def f = 1;
                def s = 1;
                
                def i = 0;
                while i < 10 then {
                    print f;

                    def tmp = f + s;
                    f = s;
                    s = tmp;

                    i = i + 1;
                }
                """, "1\n1\n2\n3\n5\n8\n13\n21\n34\n55\n")
        );
    }

    @BackendTest(JVM)
    void perf(Compiler compiler) {
        //java 1B   420  426  459  423  421
        //java 10B 4417 4405 4451 4381 4397
        //ib   1B   558  559  593  554  550
        //ib   10B 4553 4531 4548 4535 4541
        compiler.execute("""
            def a: i64 = 1;
            def b: i64 = 1;
            
            def i: i64 = 0;
            while i < 1000000000 then {
                def tmp = a + b;

                a = b;
                b = tmp;

                i = i + 1;
            }
            
            print b;
            """, "1601461754847896408\n");
    }
}
