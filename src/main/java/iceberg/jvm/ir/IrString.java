package iceberg.jvm.ir;

public class IrString extends IrExpression {

    public final String value;

    public IrString(String value) {
        super(IcebergType.string);
        this.value = value;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrString(this);
    }
}
