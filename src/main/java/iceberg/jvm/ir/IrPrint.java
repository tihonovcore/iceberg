package iceberg.jvm.ir;

import java.util.Arrays;
import java.util.List;

public class IrPrint implements IR {

    public final IrFunction function;
    //TODO: зачем несколько аргументов?
    public final List<IrExpression> arguments;

    public IrPrint(
        IrFunction function,
        IrExpression... arguments
    ) {
        this.function = function;
        this.arguments = Arrays.stream(arguments).toList();
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrPrint(this);
    }
}
