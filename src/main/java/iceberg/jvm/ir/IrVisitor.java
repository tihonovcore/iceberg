package iceberg.jvm.ir;

public interface IrVisitor {

    void visitIrBody(IrBody irBody);
    void visitIrSuperCall(IrSuperCall irSuperCall);
    void visitIrReturn(IrReturn irReturn);
    void visitIrVariable(IrVariable irVariable);
    void visitIrStaticCall(IrStaticCall irStaticCall);
    void visitIrMethodCall(IrMethodCall irMethodCall);
    void visitIrCast(IrCast irCast);
    void visitIrNumber(IrNumber irNumber);
    void visitIrReadVariable(IrReadVariable irReadVariable);
    void visitIrUnaryExpression(IrUnaryExpression irExpression);
    void visitIrBinaryExpression(IrBinaryExpression irExpression);
    void visitIrBool(IrBool irBool);
    void visitIrString(IrString irString);
}
