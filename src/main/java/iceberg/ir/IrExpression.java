package iceberg.ir;

public abstract class IrExpression implements IR {

    public final IcebergType type;

    protected IrExpression(IcebergType type) {
        this.type = type;
    }
}
