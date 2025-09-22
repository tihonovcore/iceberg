package iceberg;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public class SemanticException extends RuntimeException {

    public int start = -1;
    public int stop = -1;

    @Deprecated
    public SemanticException(String message) {
        super(message);
    }

    public SemanticException(String message, ParserRuleContext ctx) {
        super(message);

        this.start = ctx.getStart().getStartIndex();
        this.stop = ctx.getStop().getStopIndex() + 1;
    }

    public SemanticException(String message, TerminalNode node) {
        super(message);

        this.start = node.getSymbol().getStartIndex();
        this.stop = node.getSymbol().getStopIndex() + 1;
    }
}
