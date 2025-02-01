package iceberg.jvm.ir;

public class IrLoop implements IR {

    public final IrExpression condition;
    public final IR body;

    public IrLoop(IrExpression condition, IR body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrLoop(this);
    }
}
