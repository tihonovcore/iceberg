package iceberg.ir;

public class IrPrint implements IR {

    public final IrFunction function;
    public final IrExpression argument;

    public IrPrint(IrFunction function, IrExpression argument) {
        this.function = function;
        this.argument = argument;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrPrint(this);
    }
}
