import iceberg.ParsingUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ParserTest {

    @Test
    void positive() {
        var file = ParsingUtil.parse("print 20; print 0; print 1;");
        assertThat(file.printStatement())
            .hasSize(3)
            .allMatch(print -> Integer.parseInt(print.expression().getText()) >= 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "print print;", "print -10;", "print 10", "print 0001;"
    })
    void negative(String source) {
        assertThrows(IllegalArgumentException.class, () -> ParsingUtil.parse(source));
    }
}
