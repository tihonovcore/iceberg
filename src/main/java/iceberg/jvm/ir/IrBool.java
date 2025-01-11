package iceberg.jvm.ir;

public class IrBool extends IrExpression {

    public final boolean value;

    public IrBool(boolean value) {
        super(IcebergType.bool);
        this.value = value;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrBool(this);
    }
}
