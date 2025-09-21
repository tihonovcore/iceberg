package fe;

import iceberg.antlr.IcebergParser;
import iceberg.common.phases.ParseSourcePhase;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionsTest {

    private static String dump(IcebergParser.FileContext file) {
        var listener = new DumpListener();
        ParseTreeWalker.DEFAULT.walk(listener, file);

        return listener.getResult().toString();
    }

    @Test
    void noReturnType() {
        var file = new ParseSourcePhase().execute("""
            fun foo() {
                print(100);
            }
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN FunctionDefinitionStatementContext
                  fun
                  foo
                  (
                  IN ParametersContext
                  OUT ParametersContext
                  )
                  IN BlockContext
                    {
                    IN StatementContext
                      IN PrintStatementContext
                        print
                        IN AtomExpressionContext
                          IN AtomContext
                            (
                            IN AtomExpressionContext
                              IN AtomContext
                                100
                              OUT AtomContext
                            OUT AtomExpressionContext
                            )
                          OUT AtomContext
                        OUT AtomExpressionContext
                      OUT PrintStatementContext
                      ;
                    OUT StatementContext
                    }
                  OUT BlockContext
                OUT FunctionDefinitionStatementContext
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void returnType() {
        var file = new ParseSourcePhase().execute("""
            fun foo(): i32 {
                return 10 + 10;
            }
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN FunctionDefinitionStatementContext
                  fun
                  foo
                  (
                  IN ParametersContext
                  OUT ParametersContext
                  )
                  :
                  i32
                  IN BlockContext
                    {
                    IN StatementContext
                      IN ReturnStatementContext
                        return
                        IN AdditionExpressionContext
                          IN AtomExpressionContext
                            IN AtomContext
                              10
                            OUT AtomContext
                          OUT AtomExpressionContext
                          +
                          IN AtomExpressionContext
                            IN AtomContext
                              10
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT AdditionExpressionContext
                      OUT ReturnStatementContext
                      ;
                    OUT StatementContext
                    }
                  OUT BlockContext
                OUT FunctionDefinitionStatementContext
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void parameters() {
        var file = new ParseSourcePhase().execute("""
            fun foo(a: i32, b: i32): i32 {
                return a + b;
            }
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN FunctionDefinitionStatementContext
                  fun
                  foo
                  (
                  IN ParametersContext
                    IN ParameterContext
                      a
                      :
                      i32
                    OUT ParameterContext
                    ,
                    IN ParameterContext
                      b
                      :
                      i32
                    OUT ParameterContext
                  OUT ParametersContext
                  )
                  :
                  i32
                  IN BlockContext
                    {
                    IN StatementContext
                      IN ReturnStatementContext
                        return
                        IN AdditionExpressionContext
                          IN AtomExpressionContext
                            IN AtomContext
                              a
                            OUT AtomContext
                          OUT AtomExpressionContext
                          +
                          IN AtomExpressionContext
                            IN AtomContext
                              b
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT AdditionExpressionContext
                      OUT ReturnStatementContext
                      ;
                    OUT StatementContext
                    }
                  OUT BlockContext
                OUT FunctionDefinitionStatementContext
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void call() {
        var file = new ParseSourcePhase().execute("""
            foo();
            def f = foo();
            
            bar(1, 2, "str");
            def b = bar(1, 2, "str");
            
            print 3 + qux(f + b);
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN AtomExpressionContext
                  IN AtomContext
                    IN FunctionCallContext
                      foo
                      (
                      IN ArgumentsContext
                      OUT ArgumentsContext
                      )
                    OUT FunctionCallContext
                  OUT AtomContext
                OUT AtomExpressionContext
                ;
              OUT StatementContext
              IN StatementContext
                IN DefStatementContext
                  def
                  f
                  =
                  IN AtomExpressionContext
                    IN AtomContext
                      IN FunctionCallContext
                        foo
                        (
                        IN ArgumentsContext
                        OUT ArgumentsContext
                        )
                      OUT FunctionCallContext
                    OUT AtomContext
                  OUT AtomExpressionContext
                OUT DefStatementContext
                ;
              OUT StatementContext
              IN StatementContext
                IN AtomExpressionContext
                  IN AtomContext
                    IN FunctionCallContext
                      bar
                      (
                      IN ArgumentsContext
                        IN AtomExpressionContext
                          IN AtomContext
                            1
                          OUT AtomContext
                        OUT AtomExpressionContext
                        ,
                        IN AtomExpressionContext
                          IN AtomContext
                            2
                          OUT AtomContext
                        OUT AtomExpressionContext
                        ,
                        IN AtomExpressionContext
                          IN AtomContext
                            "str"
                          OUT AtomContext
                        OUT AtomExpressionContext
                      OUT ArgumentsContext
                      )
                    OUT FunctionCallContext
                  OUT AtomContext
                OUT AtomExpressionContext
                ;
              OUT StatementContext
              IN StatementContext
                IN DefStatementContext
                  def
                  b
                  =
                  IN AtomExpressionContext
                    IN AtomContext
                      IN FunctionCallContext
                        bar
                        (
                        IN ArgumentsContext
                          IN AtomExpressionContext
                            IN AtomContext
                              1
                            OUT AtomContext
                          OUT AtomExpressionContext
                          ,
                          IN AtomExpressionContext
                            IN AtomContext
                              2
                            OUT AtomContext
                          OUT AtomExpressionContext
                          ,
                          IN AtomExpressionContext
                            IN AtomContext
                              "str"
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT ArgumentsContext
                        )
                      OUT FunctionCallContext
                    OUT AtomContext
                  OUT AtomExpressionContext
                OUT DefStatementContext
                ;
              OUT StatementContext
              IN StatementContext
                IN PrintStatementContext
                  print
                  IN AdditionExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        3
                      OUT AtomContext
                    OUT AtomExpressionContext
                    +
                    IN AtomExpressionContext
                      IN AtomContext
                        IN FunctionCallContext
                          qux
                          (
                          IN ArgumentsContext
                            IN AdditionExpressionContext
                              IN AtomExpressionContext
                                IN AtomContext
                                  f
                                OUT AtomContext
                              OUT AtomExpressionContext
                              +
                              IN AtomExpressionContext
                                IN AtomContext
                                  b
                                OUT AtomContext
                              OUT AtomExpressionContext
                            OUT AdditionExpressionContext
                          OUT ArgumentsContext
                          )
                        OUT FunctionCallContext
                      OUT AtomContext
                    OUT AtomExpressionContext
                  OUT AdditionExpressionContext
                OUT PrintStatementContext
                ;
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }
}
