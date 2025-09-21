package fe;

import iceberg.antlr.IcebergParser;
import iceberg.common.phases.ParseSourcePhase;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JumpStatementsTest {

    private static String dump(IcebergParser.FileContext file) {
        var listener = new DumpListener();
        ParseTreeWalker.DEFAULT.walk(listener, file);

        return listener.getResult().toString();
    }

    @Test
    void ifOnly() {
        var file = new ParseSourcePhase().execute("""
            if 2 > 2 + 2 then print 3000;
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN IfStatementContext
                  if
                  IN RelationalExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        2
                      OUT AtomContext
                    OUT AtomExpressionContext
                    >
                    IN AdditionExpressionContext
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                      +
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT AdditionExpressionContext
                  OUT RelationalExpressionContext
                  then
                  IN StatementContext
                    IN PrintStatementContext
                      print
                      IN AtomExpressionContext
                        IN AtomContext
                          3000
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT PrintStatementContext
                    ;
                  OUT StatementContext
                OUT IfStatementContext
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void ifElse() {
        var file = new ParseSourcePhase().execute("""
            if 2 > 2 + 2
            then print 3000;
            else print 2000;
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN IfStatementContext
                  if
                  IN RelationalExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        2
                      OUT AtomContext
                    OUT AtomExpressionContext
                    >
                    IN AdditionExpressionContext
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                      +
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT AdditionExpressionContext
                  OUT RelationalExpressionContext
                  then
                  IN StatementContext
                    IN PrintStatementContext
                      print
                      IN AtomExpressionContext
                        IN AtomContext
                          3000
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT PrintStatementContext
                    ;
                  OUT StatementContext
                  else
                  IN StatementContext
                    IN PrintStatementContext
                      print
                      IN AtomExpressionContext
                        IN AtomContext
                          2000
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT PrintStatementContext
                    ;
                  OUT StatementContext
                OUT IfStatementContext
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void ifIfElseElse() {
        var file = new ParseSourcePhase().execute("""
            if 2 > 2 + 2
            then if 3 > 3 + 3
                 then print 3000;
                 else print 2000;
            else print 1000;
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN IfStatementContext
                  if
                  IN RelationalExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        2
                      OUT AtomContext
                    OUT AtomExpressionContext
                    >
                    IN AdditionExpressionContext
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                      +
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT AdditionExpressionContext
                  OUT RelationalExpressionContext
                  then
                  IN StatementContext
                    IN IfStatementContext
                      if
                      IN RelationalExpressionContext
                        IN AtomExpressionContext
                          IN AtomContext
                            3
                          OUT AtomContext
                        OUT AtomExpressionContext
                        >
                        IN AdditionExpressionContext
                          IN AtomExpressionContext
                            IN AtomContext
                              3
                            OUT AtomContext
                          OUT AtomExpressionContext
                          +
                          IN AtomExpressionContext
                            IN AtomContext
                              3
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT AdditionExpressionContext
                      OUT RelationalExpressionContext
                      then
                      IN StatementContext
                        IN PrintStatementContext
                          print
                          IN AtomExpressionContext
                            IN AtomContext
                              3000
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                      else
                      IN StatementContext
                        IN PrintStatementContext
                          print
                          IN AtomExpressionContext
                            IN AtomContext
                              2000
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                    OUT IfStatementContext
                  OUT StatementContext
                  else
                  IN StatementContext
                    IN PrintStatementContext
                      print
                      IN AtomExpressionContext
                        IN AtomContext
                          1000
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT PrintStatementContext
                    ;
                  OUT StatementContext
                OUT IfStatementContext
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void ifElseBlocks() {
        var file = new ParseSourcePhase().execute("""
            if 2 > 2 + 2 then {
                print 3000;
                print 2000;
                print 1000;
            } else {
                print 1000;
                print 2000;
                print 3000;
            }
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN IfStatementContext
                  if
                  IN RelationalExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        2
                      OUT AtomContext
                    OUT AtomExpressionContext
                    >
                    IN AdditionExpressionContext
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                      +
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT AdditionExpressionContext
                  OUT RelationalExpressionContext
                  then
                  IN StatementContext
                    IN BlockContext
                      {
                      IN StatementContext
                        IN PrintStatementContext
                          print
                          IN AtomExpressionContext
                            IN AtomContext
                              3000
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                      IN StatementContext
                        IN PrintStatementContext
                          print
                          IN AtomExpressionContext
                            IN AtomContext
                              2000
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                      IN StatementContext
                        IN PrintStatementContext
                          print
                          IN AtomExpressionContext
                            IN AtomContext
                              1000
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                      }
                    OUT BlockContext
                  OUT StatementContext
                  else
                  IN StatementContext
                    IN BlockContext
                      {
                      IN StatementContext
                        IN PrintStatementContext
                          print
                          IN AtomExpressionContext
                            IN AtomContext
                              1000
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                      IN StatementContext
                        IN PrintStatementContext
                          print
                          IN AtomExpressionContext
                            IN AtomContext
                              2000
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                      IN StatementContext
                        IN PrintStatementContext
                          print
                          IN AtomExpressionContext
                            IN AtomContext
                              3000
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                      }
                    OUT BlockContext
                  OUT StatementContext
                OUT IfStatementContext
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void simpleWhile() {
        var file = new ParseSourcePhase().execute("""
            while 2 > 2 + 2 then print "foo";
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN WhileStatementContext
                  while
                  IN RelationalExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        2
                      OUT AtomContext
                    OUT AtomExpressionContext
                    >
                    IN AdditionExpressionContext
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                      +
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT AdditionExpressionContext
                  OUT RelationalExpressionContext
                  then
                  IN StatementContext
                    IN PrintStatementContext
                      print
                      IN AtomExpressionContext
                        IN AtomContext
                          "foo"
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT PrintStatementContext
                    ;
                  OUT StatementContext
                OUT WhileStatementContext
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void whileBlock() {
        var file = new ParseSourcePhase().execute("""
            while 2 > 2 + 2 then {
                print "foo";
                print "bar";
            }
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN WhileStatementContext
                  while
                  IN RelationalExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        2
                      OUT AtomContext
                    OUT AtomExpressionContext
                    >
                    IN AdditionExpressionContext
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                      +
                      IN AtomExpressionContext
                        IN AtomContext
                          2
                        OUT AtomContext
                      OUT AtomExpressionContext
                    OUT AdditionExpressionContext
                  OUT RelationalExpressionContext
                  then
                  IN StatementContext
                    IN BlockContext
                      {
                      IN StatementContext
                        IN PrintStatementContext
                          print
                          IN AtomExpressionContext
                            IN AtomContext
                              "foo"
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                      IN StatementContext
                        IN PrintStatementContext
                          print
                          IN AtomExpressionContext
                            IN AtomContext
                              "bar"
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                      }
                    OUT BlockContext
                  OUT StatementContext
                OUT WhileStatementContext
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }
}
