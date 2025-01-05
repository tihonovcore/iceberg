package iceberg;

import antlr.IcebergBaseListener;
import antlr.IcebergLexer;
import antlr.IcebergParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class Main {

    public static void main(String[] args) {
        var lexer = new IcebergLexer(CharStreams.fromString("print 20; print 0; print 1;"));
        var tokens = new CommonTokenStream(lexer);
        var parser = new IcebergParser(tokens);
        var tree = parser.root();

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
        }, tree);
    }
}
