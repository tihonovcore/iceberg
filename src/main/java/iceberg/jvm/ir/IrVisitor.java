package iceberg.jvm.ir;

public interface IrVisitor {

    void visitIrFile(IrFile irFile);
    void visitIrClass(IrClass irClass);
    void visitIrNew(IrNew irNew);
    void visitIrFunction(IrFunction irFunction);
    void visitIrBody(IrBody irBody);
    void visitIrSuperCall(IrSuperCall irSuperCall);
    void visitIrReturn(IrReturn irReturn);
    void visitIrVariable(IrVariable irVariable);
    void visitIrField(IrField irField);
    void visitIrPrint(IrPrint irPrint);
    void visitIrStaticCall(IrStaticCall irStaticCall);
    void visitIrMethodCall(IrMethodCall irMethodCall);
    void visitIrIfStatement(IrIfStatement irIfStatement);
    void visitIrLoop(IrLoop irLoop);
    void visitIrCast(IrCast irCast);
    void visitIrNumber(IrNumber irNumber);
    void visitIrReadVariable(IrReadVariable irReadVariable);
    void visitIrAssignVariable(IrAssignVariable irAssignVariable);
    void visitIrGetField(IrGetField irGetField);
    void visitIrPutField(IrPutField irPutField);
    void visitIrUnaryExpression(IrUnaryExpression irExpression);
    void visitIrBinaryExpression(IrBinaryExpression irExpression);
    void visitIrBool(IrBool irBool);
    void visitIrString(IrString irString);
    void visitIrThis(IrThis irThis);
}
