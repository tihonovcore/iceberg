package e2e;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class ClassesTest extends Base {

    @ParameterizedTest
    @MethodSource
    void definitions(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> definitions() {
        return Stream.of(
            Arguments.of("""
                class Foo {
                }
                
                print "hello";""", "hello\n")
//            Arguments.of("""
//                class Foo {
//                    fun sayFoo() {
//                        print "foo";
//                    }
//                }
//
//                print "hello";""", "hello\n")
        );
    }

}
