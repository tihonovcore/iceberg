package iceberg.jvm.ir;

import java.util.Arrays;
import java.util.List;

public class IrMethodCall extends IrExpression {

    public final IrExpression receiver;
    public final IrFunction function;
    public final List<IrExpression> arguments;

    public IrMethodCall(
        IrExpression receiver,
        IrFunction function,
        IrExpression... arguments
    ) {
        super(function.returnType);
        this.receiver = receiver;
        this.function = function;
        this.arguments = Arrays.stream(arguments).toList();
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrMethodCall(this);
    }
}
