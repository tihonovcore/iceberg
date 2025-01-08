package iceberg.jvm.phases;

import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;

public class FillConstantPoolPhase implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        file.accept(new IcebergBaseVisitor<>() {
            @Override
            public Object visitExpression(IcebergParser.ExpressionContext ctx) {
                var value = Integer.parseInt(ctx.NUMBER().getText());
                if (value < Short.MIN_VALUE || Short.MAX_VALUE < value) {
                    unit.constantPool.addInteger(value);
                }
                return super.visitExpression(ctx);
            }
        });
    }
}
