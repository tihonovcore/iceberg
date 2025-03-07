package e2e;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class ConditionalStatementTest extends Base {

    @ParameterizedTest
    @MethodSource
    void ifOnly(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> ifOnly() {
        return Stream.of(
            Arguments.of("""
                if 3 > 2 then print 100;
                """, "100\n"),
            Arguments.of("""
                if 3 < 2 then print 100;
                """, ""),
            Arguments.of("""
                def x = 100;
                if x > 0 then print 2 * x;
                """, "200\n"),
            Arguments.of("""
                def x = 100;
                if x > 0 then print 2 * x;
                def y = 200;
                if y > x then print x * y;
                """, "200\n20000\n"),
            Arguments.of("""
                def x = 100;
                if x > 0 then {
                    print 2 * x;
                    def y = 200;
                    if true then print y / x;
                }
                """, "200\n2\n"),
            Arguments.of("""
                def x = 100;
                if x > 0 then {
                    print 2 * x;
                    def y = 200;
                    if true then print y / x;
                }
               
                def y = 20000;
                if true then print y / x;
                """, "200\n2\n200\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void ifElse(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> ifElse() {
        return Stream.of(
            Arguments.of("""
                if 3 > 2 then print 100;
                else print 200;
                
                if 3 <= 2 then print 100;
                else print 200;
                """, "100\n200\n"),
            Arguments.of("""
                def x = 100;
                if x > 0 then {
                    def y = 200;
                    print x * y;
                } else {
                    def z = 300;
                    print x * z;
                }
                """, "20000\n"),
            Arguments.of("""
                def x = 10;
                print x;
                
                if x > 0 then {
                    x = x * 99;
                } else {
                    x = 0;
                }
                
                print x;
                """, "10\n990\n"),
            Arguments.of("""
                def x = 10;
                print x;
                
                if x > 0 then {
                    x = x * 99;
                } else {
                    x = 0;
                }
                
                def y = x;
                if true then print x + y;
                """, "10\n1980\n")
        );
    }
}
