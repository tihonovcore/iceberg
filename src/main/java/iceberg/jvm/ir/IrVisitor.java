package iceberg.jvm.ir;

public interface IrVisitor {

    void visitIrBody(IrBody irBody);
    void visitIrSuperCall(IrSuperCall irSuperCall);
    void visitReturn(IrReturn irReturn);
}
