package fe;

import iceberg.antlr.IcebergParser;
import iceberg.fe.ParsingUtil;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefStatementsTest {

    private static String dump(IcebergParser.FileContext file) {
        var listener = new DumpListener();
        ParseTreeWalker.DEFAULT.walk(listener, file);

        return listener.getResult().toString();
    }

    @Test
    void defWithInit() {
        var file = ParsingUtil.parse("""
            def x = 100;
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN DefStatementContext
                  def
                  x
                  =
                  IN AtomExpressionContext
                    IN AtomContext
                      100
                    OUT AtomContext
                  OUT AtomExpressionContext
                OUT DefStatementContext
                ;
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void defWithType() {
        var file = ParsingUtil.parse("""
            def x: bool;
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN DefStatementContext
                  def
                  x
                  :
                  bool
                OUT DefStatementContext
                ;
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void defWithTypeAndInit() {
        var file = ParsingUtil.parse("""
            def x: bool = true or false;
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN DefStatementContext
                  def
                  x
                  :
                  bool
                  =
                  IN LogicalOrExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        true
                      OUT AtomContext
                    OUT AtomExpressionContext
                    or
                    IN AtomExpressionContext
                      IN AtomContext
                        false
                      OUT AtomContext
                    OUT AtomExpressionContext
                  OUT LogicalOrExpressionContext
                OUT DefStatementContext
                ;
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }
}
