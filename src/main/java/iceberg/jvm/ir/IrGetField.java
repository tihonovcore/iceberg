package iceberg.jvm.ir;

public class IrGetField extends IrExpression {

    public final IrExpression receiver;
    public final IrField irField;

    public IrGetField(IrExpression receiver, IrField irField) {
        super(irField.type);
        this.receiver = receiver;
        this.irField = irField;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrGetField(this);
    }
}
