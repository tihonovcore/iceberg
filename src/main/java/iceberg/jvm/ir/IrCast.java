package iceberg.jvm.ir;

public class IrCast extends IrExpression {

    public final IrExpression irExpression;

    public IrCast(IrExpression irExpression, IcebergType type) {
        super(type);
        this.irExpression = irExpression;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrCast(this);
    }
}
