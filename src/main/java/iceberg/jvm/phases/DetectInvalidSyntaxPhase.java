package iceberg.jvm.phases;

import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergParser;
import iceberg.fe.CompilationException;
import iceberg.jvm.CompilationUnit;

public class DetectInvalidSyntaxPhase implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        file.accept(new IcebergBaseVisitor<>() {
            @Override
            public Object visitUnaryMinusExpression(IcebergParser.UnaryMinusExpressionContext ctx) {
                var number = ctx.atom().NUMBER();
                if (number != null && number.getText().startsWith("-")) {
                    throw new CompilationException("invalid syntax");
                }

                return super.visitUnaryMinusExpression(ctx);
            }
        });
    }
}
