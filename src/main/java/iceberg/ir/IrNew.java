package iceberg.ir;

//TODO: support parameters?
public class IrNew extends IrExpression {

    public final IrClass irClass;
//    public final List<IrVariable> parameters = new ArrayList<>();

    public IrNew(IcebergType type) {
        super(type);
        this.irClass = type.irClass;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrNew(this);
    }
}
