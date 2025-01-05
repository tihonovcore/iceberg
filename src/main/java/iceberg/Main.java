package iceberg;

import antlr.IcebergBaseListener;
import antlr.IcebergParser;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class Main {

    public static void main(String[] args) {
        var file = ParsingUtil.parse("print 20; print 0; print 1;");

        var walker = new ParseTreeWalker();
        walker.walk(new IcebergBaseListener() {
            @Override
            public void enterPrintStatement(IcebergParser.PrintStatementContext ctx) {
                System.out.println("enterPrintStatement");
            }

            @Override
            public void exitPrintStatement(IcebergParser.PrintStatementContext ctx) {
                System.out.println("exitPrintStatement");
            }

            @Override
            public void enterExpression(IcebergParser.ExpressionContext ctx) {
                System.out.println("enterExpression: " + ctx.getText());
            }

            @Override
            public void exitExpression(IcebergParser.ExpressionContext ctx) {
                System.out.println("exitExpression: " + ctx.getText());
            }
        }, file);
    }
}
