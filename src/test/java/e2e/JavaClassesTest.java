package e2e;

import run.BackendTest;
import run.compiler.Compiler;

import static run.BackendTarget.JVM;

public class JavaClassesTest {

    @BackendTest(JVM)
    void create(Compiler compiler) {
        compiler.execute("""
            import java.util.ArrayList;
            
            def list = new ArrayList;
            print list;
            """, "[]\n");
    }

    @BackendTest(JVM)
    void add(Compiler compiler) {
        compiler.execute("""
            import java.util.ArrayList;
            
            def list = new ArrayList;
            list.add("10");
            list.add("20");
            list.add("30");
            
            print list;
            """, "[10, 20, 30]\n");
    }

    @BackendTest(JVM)
    void sublist(Compiler compiler) {
        compiler.execute("""
            import java.util.ArrayList;
            
            def list = new ArrayList;
            list.add("10");
            list.add("20");
            list.add("30");
            
            def sub = list.subList(1, 3);
            print sub;
            """, "[20, 30]\n");
    }
}
