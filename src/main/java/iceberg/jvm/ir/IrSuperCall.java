package iceberg.jvm.ir;

import iceberg.jvm.cp.MethodRef;

public class IrSuperCall implements IR {

    public final MethodRef methodRef;

    public IrSuperCall(MethodRef methodRef) {
        this.methodRef = methodRef;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrSuperCall(this);
    }
}
