package iceberg.fe;

import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

class ErrorListener extends BaseErrorListener {

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