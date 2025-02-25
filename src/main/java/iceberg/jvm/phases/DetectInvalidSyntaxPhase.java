package iceberg.jvm.phases;

import iceberg.SemanticException;
import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;

public class DetectInvalidSyntaxPhase implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        file.accept(new IcebergBaseVisitor<>() {

            //TODO: return вне функций
            //TODO: функции внутри функций
            //TODO: вызов несуществующих функций

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
