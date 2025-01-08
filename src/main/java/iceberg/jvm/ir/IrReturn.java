package iceberg.jvm.ir;

public class IrReturn implements IR {

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrReturn(this);
    }
}
