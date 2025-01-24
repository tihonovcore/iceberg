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
    void withInit(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> withInit() {
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

        //todo: bool
        //todo: i64
        //todo: string

        //todo: type + init
        //todo: type and init later

        //todo: type=i64 and init is i32
    }

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
