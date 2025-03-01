package iceberg.jvm.ir;

import java.util.Arrays;
import java.util.List;

public class IrStaticCall extends IrExpression {

    public final IrFunction function;
    public final List<IrExpression> arguments;

    public IrStaticCall(
        IrFunction function,
        IrExpression... arguments
    ) {
        super(function.returnType);
        this.function = function;
        this.arguments = Arrays.stream(arguments).toList();
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrStaticCall(this);
    }
}
