package iceberg.jvm.ir;

import java.util.Arrays;
import java.util.List;

public class IrMethodCall extends IrExpression {

    public final IrFunction function;
    public final IrExpression receiver;
    public final List<IrExpression> arguments;

    public IrMethodCall(
        IrFunction function,
        IrExpression receiver,
        IrExpression... arguments
    ) {
        super(function.returnType);
        this.function = function;
        this.receiver = receiver;
        this.arguments = Arrays.stream(arguments).toList();
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrMethodCall(this);
    }
}
