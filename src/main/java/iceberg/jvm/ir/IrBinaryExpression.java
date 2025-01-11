package iceberg.jvm.ir;

public class IrBinaryExpression extends IrExpression {

    public final IrExpression left;
    public final IrExpression right;
    public final IcebergBinaryOperator operator;

    public IrBinaryExpression(
        IrExpression left,
        IrExpression right,
        IcebergBinaryOperator operator,
        IcebergType resultType
    ) {
        super(resultType);
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrBinaryExpression(this);
    }
}
