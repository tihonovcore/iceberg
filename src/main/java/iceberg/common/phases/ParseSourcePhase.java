package iceberg.common.phases;

import iceberg.CompilationException;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import org.antlr.v4.runtime.*;

public class ParseSourcePhase {

    public IcebergParser.FileContext execute(String source) {
        var listener = new ErrorListener();

        var lexer = new IcebergLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);

        var tokens = new CommonTokenStream(lexer);

        var parser = new IcebergParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);

        return parser.file();
    }

    static class ErrorListener extends BaseErrorListener {

        @Override
        public void syntaxError(
            Recognizer<?, ?> recognizer, Object offendingSymbol,
            int line, int charPositionInLine,
            String msg, RecognitionException e
        ) {
            //TODO: make better error messages
            var input = recognizer instanceof IcebergParser parser
                ? parser.getInputStream().getTokenSource().getInputStream().toString()
                : ((IcebergLexer) recognizer).getInputStream().toString();

            throw new CompilationException(msg + " at %d:%d".formatted(line, charPositionInLine), e);
        }
    }
}
