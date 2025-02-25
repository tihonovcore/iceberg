package iceberg.jvm.ir;

import iceberg.jvm.cp.MethodRef;

import java.util.Arrays;
import java.util.List;

public class IrStaticCall extends IrExpression {

    public final MethodRef methodRef;
    public final List<IrExpression> arguments;

    public IrStaticCall(
        IcebergType returnType,
        MethodRef methodRef,
        IrExpression... arguments
    ) {
        super(returnType);
        this.methodRef = methodRef;
        this.arguments = Arrays.stream(arguments).toList();
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrStaticCall(this);
    }
}
