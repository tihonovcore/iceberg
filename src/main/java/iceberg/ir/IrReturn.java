package iceberg.ir;

import org.jetbrains.annotations.Nullable;

public class IrReturn implements IR {

    public final @Nullable IrExpression expression;

    public IrReturn() {
        this.expression = null;
    }

    public IrReturn(@Nullable IrExpression expression) {
        this.expression = expression;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrReturn(this);
    }
}
