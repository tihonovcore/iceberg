package iceberg.jvm.ir;

public class IrNumber implements IrExpression {

    public final long value;
    public final IcebergType type;

    public IrNumber(long value) {
        this.value = value;
        this.type = Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE
            ? IcebergType.i32
            : IcebergType.i64;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrNumber(this);
    }
}
