package iceberg.ir;

public class IrPutField extends IrExpression {

    public final IrExpression receiver;
    public final IrField irField;
    public final IrExpression expression;

    public IrPutField(IrExpression receiver, IrField irField, IrExpression expression) {
        super(irField.type);
        this.receiver = receiver;
        this.irField = irField;
        this.expression = expression;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrPutField(this);
    }
}
