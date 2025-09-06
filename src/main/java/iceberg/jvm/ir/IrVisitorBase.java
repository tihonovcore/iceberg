package iceberg.jvm.ir;

public class IrVisitorBase implements IrVisitor {

    @Override
    public void visitIrFile(IrFile irFile) {
        irFile.classes.forEach(irClass -> irClass.accept(this));
    }

    @Override
    public void visitIrClass(IrClass irClass) {
        irClass.defaultConstructor.accept(this);
        irClass.methods.forEach(irFunction -> irFunction.accept(this));
        irClass.fields.values().forEach(irField -> irField.accept(this));
    }

    @Override
    public void visitIrNew(IrNew irNew) {
        //no children
    }

    @Override
    public void visitIrFunction(IrFunction irFunction) {
        irFunction.parameters.forEach(irVariable -> irVariable.accept(this));
        irFunction.irBody.accept(this);
    }

    @Override
    public void visitIrBody(IrBody irBody) {
        irBody.statements.forEach(statement -> statement.accept(this));
    }

    @Override
    public void visitIrSuperCall(IrSuperCall irSuperCall) {
        //no children
    }

    @Override
    public void visitIrReturn(IrReturn irReturn) {
        if (irReturn.expression != null) {
            irReturn.expression.accept(this);
        }
    }

    @Override
    public void visitIrVariable(IrVariable irVariable) {
        if (irVariable.initializer != null) {
            irVariable.initializer.accept(this);
        }
    }

    @Override
    public void visitIrField(IrField irField) {
        //no children
    }

    @Override
    public void visitIrPrint(IrPrint irPrint) {
        irPrint.arguments.forEach(argument -> argument.accept(this));
    }

    @Override
    public void visitIrStaticCall(IrStaticCall irStaticCall) {
        irStaticCall.arguments.forEach(argument -> argument.accept(this));
    }

    @Override
    public void visitIrMethodCall(IrMethodCall irMethodCall) {
        irMethodCall.receiver.accept(this);
        irMethodCall.arguments.forEach(argument -> argument.accept(this));
    }

    @Override
    public void visitIrIfStatement(IrIfStatement irIfStatement) {
        irIfStatement.condition.accept(this);
        irIfStatement.thenStatement.accept(this);
        if (irIfStatement.elseStatement != null) {
            irIfStatement.elseStatement.accept(this);
        }
    }

    @Override
    public void visitIrLoop(IrLoop irLoop) {
        irLoop.condition.accept(this);
        irLoop.body.accept(this);
    }

    @Override
    public void visitIrCast(IrCast irCast) {
        irCast.irExpression.accept(this);
    }

    @Override
    public void visitIrNumber(IrNumber irNumber) {
        //no children
    }

    @Override
    public void visitIrReadVariable(IrReadVariable irReadVariable) {
        //no children
    }

    @Override
    public void visitIrAssignVariable(IrAssignVariable irAssignVariable) {
        irAssignVariable.expression.accept(this);
    }

    @Override
    public void visitIrGetField(IrGetField irGetField) {
        irGetField.receiver.accept(this);
    }

    @Override
    public void visitIrPutField(IrPutField irPutField) {
        irPutField.receiver.accept(this);
        irPutField.expression.accept(this);
    }

    @Override
    public void visitIrUnaryExpression(IrUnaryExpression irExpression) {
        irExpression.value.accept(this);
    }

    @Override
    public void visitIrBinaryExpression(IrBinaryExpression irExpression) {
        irExpression.left.accept(this);
        irExpression.right.accept(this);
    }

    @Override
    public void visitIrBool(IrBool irBool) {
        //no children
    }

    @Override
    public void visitIrString(IrString irString) {
        //no children
    }

    @Override
    public void visitIrThis(IrThis irThis) {
        //no children
    }
}
