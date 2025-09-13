package opt;

import run.BackendTest;
import run.compiler.Compiler;

import static run.BackendTarget.LLVM;

public class ConstantPropagationTest {

    //TODO: сравнить CFG до и после

    @BackendTest(LLVM)
    void test(Compiler compiler) {
        compiler.execute("""
            def i = 0;
            def x = 1;
            def y = 1;
            
            while (i < 10) then {
                x = x + 1;
                i = i + 1;
                y = y * y;
            }
            
            def p = x + y;
            print p;
            """, "12\n");
    }
}
