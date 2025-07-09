package iceberg.jvm.ir;

public class IrField implements IR {

    public final IrClass irClass;
    public final String fieldName;
    public final IcebergType type;

    public IrField(IrClass irClass, String fieldName, IcebergType type) {
        this.irClass = irClass;
        this.fieldName = fieldName;
        this.type = type;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrField(this);
    }
}
