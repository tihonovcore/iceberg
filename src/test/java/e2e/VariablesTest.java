package e2e;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class VariablesTest extends Base {

    @ParameterizedTest
    @MethodSource
    void def(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> def() {
        return Stream.of(
            Arguments.of("""
                def x = 10;
                print x;
                """, "10\n")
        );
    }
}
