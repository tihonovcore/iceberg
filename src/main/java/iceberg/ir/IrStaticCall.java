package iceberg.ir;

import java.util.List;

public class IrStaticCall extends IrExpression {

    public final IrFunction function;
    public final List<IrExpression> arguments;

    public IrStaticCall(
        IrFunction function,
        List<IrExpression> arguments
    ) {
        super(function.returnType);
        this.function = function;
        this.arguments = arguments;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrStaticCall(this);
    }
}
