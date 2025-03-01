package iceberg.jvm.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.target.CompilationUnit;

public class DetectInvalidSyntaxPhase implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
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

                throw new SemanticException("bad l-value");
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

                throw new SemanticException("not a statement");
            }

            @Override
            public Object visitUnaryMinusExpression(IcebergParser.UnaryMinusExpressionContext ctx) {
                var number = ctx.atom().NUMBER();
                if (number != null && number.getText().startsWith("-")) {
                    throw new SemanticException("invalid syntax");
                }

                return super.visitUnaryMinusExpression(ctx);
            }
        });
    }
}
