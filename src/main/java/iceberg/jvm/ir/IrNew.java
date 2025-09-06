package iceberg.jvm.ir;

//TODO: support parameters?
public class IrNew extends IrExpression {

    public final IrClass irClass;
//    public final List<IrVariable> parameters = new ArrayList<>();

    public IrNew(IrClass irClass) {
        super(new IcebergType(irClass));
        this.irClass = irClass;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrNew(this);
    }
}
