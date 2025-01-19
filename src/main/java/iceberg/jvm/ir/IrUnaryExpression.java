package iceberg.jvm.ir;

public class IrUnaryExpression extends IrExpression {

    public final IrExpression value;
    public final IcebergUnaryOperator operator;

    public IrUnaryExpression(
        IrExpression value,
        IcebergUnaryOperator operator,
        IcebergType resultType
    ) {
        super(resultType);
        this.value = value;
        this.operator = operator;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrUnaryExpression(this);
    }
}
