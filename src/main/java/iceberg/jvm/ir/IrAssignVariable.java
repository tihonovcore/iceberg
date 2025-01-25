package iceberg.jvm.ir;

public class IrAssignVariable implements IR {

    public final IrVariable definition;
    public final IrExpression expression;

    public IrAssignVariable(IrVariable definition, IrExpression expression) {
        this.definition = definition;
        this.expression = expression;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrAssignVariable(this);
    }
}
