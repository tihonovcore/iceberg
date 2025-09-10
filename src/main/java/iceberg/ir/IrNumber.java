package iceberg.ir;

public class IrNumber extends IrExpression {

    public final long value;

    public IrNumber(long value) {
        super(Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE
            ? IcebergType.i32
            : IcebergType.i64);
        this.value = value;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrNumber(this);
    }
}
