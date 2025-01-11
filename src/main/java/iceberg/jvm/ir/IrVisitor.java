package iceberg.jvm.ir;

public interface IrVisitor {

    void visitIrBody(IrBody irBody);
    void visitIrSuperCall(IrSuperCall irSuperCall);
    void visitIrReturn(IrReturn irReturn);
    void visitIrStaticCall(IrStaticCall irStaticCall);
    void visitIrNumber(IrNumber irNumber);
    void visitIrBinaryExpression(IrBinaryExpression irExpression);
    void visitIrBool(IrBool irBool);
    void visitIrString(IrString irString);
}
