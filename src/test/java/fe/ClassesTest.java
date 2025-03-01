package fe;

import iceberg.antlr.IcebergParser;
import iceberg.fe.ParsingUtil;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassesTest {

    private static String dump(IcebergParser.FileContext file) {
        var listener = new DumpListener();
        ParseTreeWalker.DEFAULT.walk(listener, file);

        return listener.getResult().toString();
    }

    @Test
    void classDefinition() {
        var file = ParsingUtil.parse("""
            class Foo {
                def x: i32
                def y = "string"

                fun show() {
                    print x;
                    print y;
                }
            }
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN ClassDefinitionStatementContext
                  class
                  Foo
                  {
                  IN DefStatementContext
                    def
                    x
                    :
                    i32
                  OUT DefStatementContext
                  IN DefStatementContext
                    def
                    y
                    =
                    IN AtomExpressionContext
                      IN AtomContext
                        "string"
                      OUT AtomContext
                    OUT AtomExpressionContext
                  OUT DefStatementContext
                  IN FunctionDefinitionStatementContext
                    fun
                    show
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
                              x
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
                              y
                            OUT AtomContext
                          OUT AtomExpressionContext
                        OUT PrintStatementContext
                        ;
                      OUT StatementContext
                      }
                    OUT BlockContext
                  OUT FunctionDefinitionStatementContext
                  }
                OUT ClassDefinitionStatementContext
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void read() {
        var file = ParsingUtil.parse("""
            print user.salary + user.bonuses();
            
            bar("user", user, user.age, user.findRelatives());
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN PrintStatementContext
                  print
                  IN AdditionExpressionContext
                    IN MemberExpressionContext
                      IN AtomExpressionContext
                        IN AtomContext
                          user
                        OUT AtomContext
                      OUT AtomExpressionContext
                      .
                      salary
                    OUT MemberExpressionContext
                    +
                    IN MemberExpressionContext
                      IN AtomExpressionContext
                        IN AtomContext
                          user
                        OUT AtomContext
                      OUT AtomExpressionContext
                      .
                      IN FunctionCallContext
                        bonuses
                        (
                        IN ArgumentsContext
                        OUT ArgumentsContext
                        )
                      OUT FunctionCallContext
                    OUT MemberExpressionContext
                  OUT AdditionExpressionContext
                OUT PrintStatementContext
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
                            "user"
                          OUT AtomContext
                        OUT AtomExpressionContext
                        ,
                        IN AtomExpressionContext
                          IN AtomContext
                            user
                          OUT AtomContext
                        OUT AtomExpressionContext
                        ,
                        IN MemberExpressionContext
                          IN AtomExpressionContext
                            IN AtomContext
                              user
                            OUT AtomContext
                          OUT AtomExpressionContext
                          .
                          age
                        OUT MemberExpressionContext
                        ,
                        IN MemberExpressionContext
                          IN AtomExpressionContext
                            IN AtomContext
                              user
                            OUT AtomContext
                          OUT AtomExpressionContext
                          .
                          IN FunctionCallContext
                            findRelatives
                            (
                            IN ArgumentsContext
                            OUT ArgumentsContext
                            )
                          OUT FunctionCallContext
                        OUT MemberExpressionContext
                      OUT ArgumentsContext
                      )
                    OUT FunctionCallContext
                  OUT AtomContext
                OUT AtomExpressionContext
                ;
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }

    @Test
    void write() {
        var file = ParsingUtil.parse("""
            user.age = 100;
            user.show();
            
            user.findAddress().city = "Moscow";
            
            user.id = random.uuid();
            """);
        assertThat(dump(file)).isEqualTo("""
            IN FileContext
              IN StatementContext
                IN AssignExpressionContext
                  IN MemberExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        user
                      OUT AtomContext
                    OUT AtomExpressionContext
                    .
                    age
                  OUT MemberExpressionContext
                  =
                  IN AtomExpressionContext
                    IN AtomContext
                      100
                    OUT AtomContext
                  OUT AtomExpressionContext
                OUT AssignExpressionContext
                ;
              OUT StatementContext
              IN StatementContext
                IN MemberExpressionContext
                  IN AtomExpressionContext
                    IN AtomContext
                      user
                    OUT AtomContext
                  OUT AtomExpressionContext
                  .
                  IN FunctionCallContext
                    show
                    (
                    IN ArgumentsContext
                    OUT ArgumentsContext
                    )
                  OUT FunctionCallContext
                OUT MemberExpressionContext
                ;
              OUT StatementContext
              IN StatementContext
                IN AssignExpressionContext
                  IN MemberExpressionContext
                    IN MemberExpressionContext
                      IN AtomExpressionContext
                        IN AtomContext
                          user
                        OUT AtomContext
                      OUT AtomExpressionContext
                      .
                      IN FunctionCallContext
                        findAddress
                        (
                        IN ArgumentsContext
                        OUT ArgumentsContext
                        )
                      OUT FunctionCallContext
                    OUT MemberExpressionContext
                    .
                    city
                  OUT MemberExpressionContext
                  =
                  IN AtomExpressionContext
                    IN AtomContext
                      "Moscow"
                    OUT AtomContext
                  OUT AtomExpressionContext
                OUT AssignExpressionContext
                ;
              OUT StatementContext
              IN StatementContext
                IN AssignExpressionContext
                  IN MemberExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        user
                      OUT AtomContext
                    OUT AtomExpressionContext
                    .
                    id
                  OUT MemberExpressionContext
                  =
                  IN MemberExpressionContext
                    IN AtomExpressionContext
                      IN AtomContext
                        random
                      OUT AtomContext
                    OUT AtomExpressionContext
                    .
                    IN FunctionCallContext
                      uuid
                      (
                      IN ArgumentsContext
                      OUT ArgumentsContext
                      )
                    OUT FunctionCallContext
                  OUT MemberExpressionContext
                OUT AssignExpressionContext
                ;
              OUT StatementContext
              <EOF>
            OUT FileContext
            """);
    }
}
