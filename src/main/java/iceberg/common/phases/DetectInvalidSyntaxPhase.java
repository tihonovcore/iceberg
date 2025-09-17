package iceberg.common.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import org.antlr.v4.runtime.tree.TerminalNode;

public class DetectInvalidSyntaxPhase {

    public void execute(IcebergParser.FileContext file) {
        file.accept(new IcebergBaseVisitor<>() {

            boolean insideClass = false;
            boolean insideFunction = false;

            @Override
            public Object visitClassDefinitionStatement(IcebergParser.ClassDefinitionStatementContext ctx) {
                try {
                    insideClass = true;
                    return super.visitClassDefinitionStatement(ctx);
                } finally {
                    insideClass = false;
                }
            }

            @Override
            public Object visitFunctionDefinitionStatement(IcebergParser.FunctionDefinitionStatementContext ctx) {
                if (insideFunction) {
                    throw new SemanticException("function inside function", ctx);
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

                throw new SemanticException("bad l-value:\n" + ctx.getText(), ctx);
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

                throw new SemanticException("not a statement\n" + ctx.getText(), ctx);
            }

            @Override
            public Object visitUnaryMinusExpression(IcebergParser.UnaryMinusExpressionContext ctx) {
                var number = ctx.atom().NUMBER();
                if (number != null && number.getText().startsWith("-")) {
                    throw new SemanticException("invalid syntax", ctx);
                }

                return super.visitUnaryMinusExpression(ctx);
            }

            @Override
            public Object visitTerminal(TerminalNode node) {
                var curr = node.getSymbol().getType();
                if (curr == IcebergLexer.THIS) {
                    if (insideClass && insideFunction) {
                        return super.visitTerminal(node);
                    } else {
                        throw new SemanticException("`this` outside of member-function", node);
                    }
                }

                return super.visitTerminal(node);
            }
        });
    }
}
