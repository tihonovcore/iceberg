package iceberg.fe;

import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import org.antlr.v4.runtime.*;

public class ParsingUtil {

    public static IcebergParser.FileContext parse(String source) {
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
}
