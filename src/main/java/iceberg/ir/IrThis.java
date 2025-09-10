package iceberg.ir;

public class IrThis extends IrExpression {

    public final IrClass irClass;

    public IrThis(IrClass irClass) {
        super(new IcebergType(irClass));
        this.irClass = irClass;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrThis(this);
    }
}
