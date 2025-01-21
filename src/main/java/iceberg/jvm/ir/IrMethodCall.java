package iceberg.jvm.ir;

import iceberg.jvm.cp.MethodRef;

import java.util.Arrays;
import java.util.List;

public class IrMethodCall extends IrExpression {

    public final IrExpression receiver;
    public final MethodRef methodRef;
    public final List<IrExpression> arguments;

    public IrMethodCall(
        MethodRef methodRef,
        IcebergType returnType,
        IrExpression receiver,
        IrExpression... arguments
    ) {
        super(returnType);
        this.receiver = receiver;
        this.methodRef = methodRef;
        this.arguments = Arrays.stream(arguments).toList();
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrMethodCall(this);
    }
}
