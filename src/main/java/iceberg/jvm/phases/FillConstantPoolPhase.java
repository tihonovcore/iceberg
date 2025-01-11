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
                long value = Long.parseLong(ctx.NUMBER().getText());
                if (value < Integer.MIN_VALUE || Integer.MAX_VALUE < value) {
                    unit.constantPool.addLong(value);
                } else if (value < Short.MIN_VALUE || Short.MAX_VALUE < value) {
                    unit.constantPool.addInteger((int) value);
                }

                return super.visitExpression(ctx);
            }
        });
    }
}
