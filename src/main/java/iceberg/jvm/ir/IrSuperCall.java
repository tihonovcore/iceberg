package iceberg.jvm.ir;

public class IrSuperCall implements IR {

    public final IrFunction function;

    public IrSuperCall(IrFunction function) {
        this.function = function;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrSuperCall(this);
    }
}
