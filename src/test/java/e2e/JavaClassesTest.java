package e2e;

import org.junit.jupiter.api.Test;

public class JavaClassesTest extends Base {

    @Test
    void create() {
        execute("""
            import java.util.ArrayList;
            
            def list = new ArrayList;
            print list;
            """, "[]\n");
    }

    @Test
    void add() {
        execute("""
            import java.util.ArrayList;
            
            def list = new ArrayList;
            list.add("10");
            list.add("20");
            list.add("30");
            
            print list;
            """, "[10, 20, 30]\n");
    }

    @Test
    void sublist() {
        execute("""
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
