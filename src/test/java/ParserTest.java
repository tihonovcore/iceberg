import antlr.IcebergLexer;
import antlr.IcebergParser;
import org.antlr.v4.runtime.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserTest {

    @Test
    void positive() {
        var lexer = new IcebergLexer(CharStreams.fromString("print 20; print 0; print 1;"));
        var tokens = new CommonTokenStream(lexer);
        var parser = new IcebergParser(tokens);

        var tree = parser.root();
        assertThat(tree.printStatement())
            .hasSize(3)
            .allMatch(print -> Integer.parseInt(print.expression().getText()) >= 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "print print;", "print -10;", "print 10", "print 0001;"
    })
    void negative(String expression) {
        class Listener extends BaseErrorListener {
            public boolean hasError = false;

            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                hasError = true;
            }
        }

        var listener = new Listener();

        var lexer = new IcebergLexer(CharStreams.fromString(expression));
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);

        var tokens = new CommonTokenStream(lexer);

        var parser = new IcebergParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);

        var ignore = parser.root();

        assertThat(listener.hasError).isTrue();
    }
}
