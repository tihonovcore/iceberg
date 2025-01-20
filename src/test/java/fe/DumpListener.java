package fe;

import iceberg.antlr.IcebergBaseListener;
import lombok.Getter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

@Getter
public class DumpListener extends IcebergBaseListener {

    private final StringBuilder result = new StringBuilder();
    private int indent = 0;

    @Override
    public void visitTerminal(TerminalNode node) {
        result.append(" ".repeat(indent));
        result.append(node.getText());
        result.append(System.lineSeparator());
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        result.append(" ".repeat(indent));
        result.append("IN ");
        result.append(ctx.getClass().getSimpleName());
        result.append(System.lineSeparator());

        indent += 2;
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        indent -= 2;

        result.append(" ".repeat(indent));
        result.append("OUT ");
        result.append(ctx.getClass().getSimpleName());
        result.append(System.lineSeparator());
    }
}
