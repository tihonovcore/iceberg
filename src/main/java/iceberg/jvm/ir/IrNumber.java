package iceberg.jvm.ir;

public class IrNumber implements IrExpression {

    public final int value;

    public IrNumber(int value) {
        this.value = value;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrNumber(this);
    }
}
