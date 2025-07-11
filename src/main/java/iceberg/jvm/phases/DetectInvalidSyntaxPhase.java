package iceberg.jvm.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import org.antlr.v4.runtime.tree.TerminalNode;

public class DetectInvalidSyntaxPhase {

    public void execute(IcebergParser.FileContext file) {
        file.accept(new IcebergBaseVisitor<>() {

            boolean insideFunction = false;

            @Override
            public Object visitFunctionDefinitionStatement(IcebergParser.FunctionDefinitionStatementContext ctx) {
                if (insideFunction) {
                    throw new SemanticException("function inside function");
                }

                try {
                    insideFunction = true;
                    return super.visitFunctionDefinitionStatement(ctx);
                } finally {
                    insideFunction = false;
                }
            }

            @Override
            public Object visitAssignExpression(IcebergParser.AssignExpressionContext ctx) {
                if (ctx.left instanceof IcebergParser.AtomExpressionContext atom) {
                    if (atom.atom().ID() != null) {
                        return super.visitAssignExpression(ctx);
                    }
                }

                if (ctx.left instanceof IcebergParser.MemberExpressionContext member) {
                    if (member.functionCall() == null) {
                        return member.expression().accept(this);
                    }
                }

                throw new SemanticException("bad l-value:\n" + ctx.getText());
            }

            @Override
            public Object visitStatement(IcebergParser.StatementContext ctx) {
                var expression = ctx.expression();
                if (expression == null) {
                    return super.visitStatement(ctx);
                }

                if (expression instanceof IcebergParser.AssignExpressionContext) {
                    return super.visitStatement(ctx);
                }

                if (expression instanceof IcebergParser.AtomExpressionContext atom) {
                    if (atom.atom().functionCall() != null) {
                        return super.visitStatement(ctx);
                    }
                }

                if (expression instanceof IcebergParser.MemberExpressionContext member) {
                    if (member.functionCall() != null) {
                        return super.visitStatement(ctx);
                    }
                }

                throw new SemanticException("not a statement\n" + ctx.getText());
            }

            @Override
            public Object visitUnaryMinusExpression(IcebergParser.UnaryMinusExpressionContext ctx) {
                var number = ctx.atom().NUMBER();
                if (number != null && number.getText().startsWith("-")) {
                    throw new SemanticException("invalid syntax");
                }

                return super.visitUnaryMinusExpression(ctx);
            }

            @Override
            public Object visitTerminal(TerminalNode node) {
                //TODO: проверка не надежная - функция может быть статической
                // добавить insideClass?? или прометить статические функции флагом
                if (!insideFunction && node.getSymbol().getType() == IcebergLexer.THIS) {
                    throw new SemanticException("this outside function");
                }

                return super.visitTerminal(node);
            }
        });
    }
}
