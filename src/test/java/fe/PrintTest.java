package fe;

import iceberg.fe.ParsingUtil;
import iceberg.fe.CompilationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrintTest {

    @Test
    void numbers() {
        var file = ParsingUtil.parse("print 20; print 0; print 1; print -10;");
        assertThat(file.statement())
            .hasSize(4)
            .allMatch(stmt -> {
                try {
                    Integer.parseInt(stmt.printStatement().expression().getText());
                    return true;
                } catch (Exception ignore) {
                    return false;
                }
            });
    }

    @Test
    void skipWs() {
        var file = ParsingUtil.parse("\nprint\t\r    27 \t\n; \t");
        assertThat(file.statement()).hasSize(1);

        var expression = file.statement(0).printStatement().expression();
        assertThat(expression.getText()).isEqualTo("27");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "print false;",
        "print true;",
    })
    void booleans(String source) {
        var file = ParsingUtil.parse(source);
        assertThat(file.statement()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        """
        print "foo";
        """, """
        print "бар";
        """, """
        print "Ǡ Ѩ ɚ ȡ";
        """, """
        print "12345";
        """, """
        print "\\"foo";
        """, """
        print "\\"foo\\"";
        """, """
        print "foo\\"";
        """, """
        print "foo\\"bar";
        """, """
        print "\\nfoo";
        """, """
        print "\\nfoo\\n";
        """, """
        print "foo\\n";
        """, """
        print "foo\\nbar";
        """
    })
    void strings(String source) {
        var file = ParsingUtil.parse(source);
        assertThat(file.statement()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "print print;",
        "print 10",
        "print 0001;",
        "pRINt 10;",
    })
    void negative(String source) {
        assertThrows(CompilationException.class, () -> ParsingUtil.parse(source));
    }
}
