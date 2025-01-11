package fe;

import iceberg.fe.ParsingUtil;
import iceberg.fe.CompilationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTest {

    @Test
    void positive() {
        var file = ParsingUtil.parse("print 20; print 0; print 1; print -10;");
        assertThat(file.printStatement())
            .hasSize(4)
            .allMatch(print -> {
                try {
                    Integer.parseInt(print.expression().getText());
                    return true;
                } catch (Exception ignore) {
                    return false;
                }
            });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "print print;", "print 10", "print 0001;"
    })
    void negative(String source) {
        assertThrows(CompilationException.class, () -> ParsingUtil.parse(source));
    }
}
