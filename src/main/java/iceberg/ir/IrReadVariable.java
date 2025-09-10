package iceberg.ir;

public class IrReadVariable extends IrExpression {

    public final IrVariable definition;

    public IrReadVariable(IrVariable definition) {
        super(definition.type);
        this.definition = definition;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrReadVariable(this);
    }
}
