package iceberg;

import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import org.antlr.v4.runtime.*;

public class ParsingUtil {

    private static class Listener extends BaseErrorListener {
        //TODO: create informative error messages
        public boolean hasError = false;

        @Override
        public void syntaxError(
            Recognizer<?, ?> recognizer, Object offendingSymbol,
            int line, int charPositionInLine,
            String msg, RecognitionException e
        ) {
            hasError = true;
        }
    }

    public static IcebergParser.FileContext parse(String source) {
        var listener = new Listener();

        var lexer = new IcebergLexer(CharStreams.fromString(source));
//        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);

        var tokens = new CommonTokenStream(lexer);

        var parser = new IcebergParser(tokens);
//        parser.removeErrorListeners();
        parser.addErrorListener(listener);

        var file = parser.file();
        if (listener.hasError) {
            throw new IllegalArgumentException();
        } else {
            return file;
        }
    }
}
