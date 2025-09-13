package e2e;

import org.junit.jupiter.params.provider.Arguments;
import run.ParameterizedBackendTest;
import run.compiler.Compiler;

import java.util.stream.Stream;

import static run.BackendTarget.JVM;
import static run.BackendTarget.LLVM;

public class EqualityExpressionTest {

    @ParameterizedBackendTest({JVM, LLVM})
    void equals(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> equals() {
        return Stream.of(
            Arguments.of("print 100 == 101;", "false\n"),
            Arguments.of("print 100 == 100;", "true\n"),
            Arguments.of("print 100 == 99;", "false\n"),
            Arguments.of("print -100 == 100;", "false\n"),
            Arguments.of("print -100 == 0;", "false\n"),
            Arguments.of("print -100 == -99;", "false\n"),
            Arguments.of("print -100 == -100;", "true\n"),
            Arguments.of("print -100 == -101;", "false\n"),
            Arguments.of("print 2 + 2 == 5;", "false\n"),
            Arguments.of("print 2 * 2 + 1 == 1 * 2 + 3;", "true\n"),
            Arguments.of("print 2 == 2 + 1 and 2 + 2 == 5;", "false\n"),
            Arguments.of("print 0 == 111222333445;", "false\n"),
            Arguments.of("print 111222333444 == 0;", "false\n"),
            Arguments.of("print 111222333444 == 111222333445;", "false\n"),
            Arguments.of("print 111222333445 == 111222333445;", "true\n"),
            Arguments.of("print true == true;", "true\n"),
            Arguments.of("print true == false;", "false\n"),
            Arguments.of("print true == true and true;", "true\n"),
            Arguments.of("print true == true and false;", "false\n"),
            Arguments.of("print true or true == false;", "true\n"),
            Arguments.of("print false or true == false;", "false\n"),
            Arguments.of("print false or true == true;", "true\n")
        );
    }

    @ParameterizedBackendTest(JVM)
    void equals__strings(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> equals__strings() {
        return Stream.of(
            Arguments.of("print \"foo\" == \"foo\";", "true\n"),
            Arguments.of("print \"foo\" == \"bar\";", "false\n")
        );
    }

    @ParameterizedBackendTest({JVM, LLVM})
    void notEquals(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> notEquals() {
        return Stream.of(
            Arguments.of("print 100 != 101;", "true\n"),
            Arguments.of("print 100 != 100;", "false\n"),
            Arguments.of("print 100 != 99;", "true\n"),
            Arguments.of("print -100 != 100;", "true\n"),
            Arguments.of("print -100 != 0;", "true\n"),
            Arguments.of("print -100 != -99;", "true\n"),
            Arguments.of("print -100 != -100;", "false\n"),
            Arguments.of("print -100 != -101;", "true\n"),
            Arguments.of("print 2 + 2 != 5;", "true\n"),
            Arguments.of("print 2 * 2 + 1 != 1 * 2 + 3;", "false\n"),
            Arguments.of("print 2 != 2 + 1 and 2 + 2 != 5;", "true\n"),
            Arguments.of("print 0 != 111222333445;", "true\n"),
            Arguments.of("print 111222333444 != 0;", "true\n"),
            Arguments.of("print 111222333444 != 111222333445;", "true\n"),
            Arguments.of("print 111222333445 != 111222333445;", "false\n"),
            Arguments.of("print true != true;", "false\n"),
            Arguments.of("print true != false;", "true\n"),
            Arguments.of("print true != true and true;", "false\n"),
            Arguments.of("print true != true and false;", "false\n"),
            Arguments.of("print true or true != false;", "true\n"),
            Arguments.of("print false or true != false;", "true\n"),
            Arguments.of("print false or true != true;", "false\n")
        );
    }

    @ParameterizedBackendTest(JVM)
    void notEquals__strings(Compiler compiler, String source, String expected) {
        compiler.execute(source, expected);
    }

    @SuppressWarnings("unused")
    static Stream<Arguments> notEquals__strings() {
        return Stream.of(
            Arguments.of("print \"foo\" != \"foo\";", "false\n"),
            Arguments.of("print \"foo\" != \"bar\";", "true\n")
        );
    }
}
