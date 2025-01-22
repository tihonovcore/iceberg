package iceberg.jvm.ir;

import org.jetbrains.annotations.Nullable;

public class IrVariable implements IR {

    public final IcebergType type;
    public final @Nullable IrExpression initializer;

    public IrVariable(IcebergType type, @Nullable IrExpression initializer) {
        this.type = type;
        this.initializer = initializer;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrVariable(this);
    }
}
