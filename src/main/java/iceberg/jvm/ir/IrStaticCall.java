package iceberg.jvm.ir;

import iceberg.jvm.cp.FieldRef;
import iceberg.jvm.cp.MethodRef;

import java.util.Arrays;
import java.util.List;

public class IrStaticCall implements IR {

    public final FieldRef fieldRef;
    public final MethodRef methodRef;
    public final List<IrExpression> arguments;

    public IrStaticCall(FieldRef fieldRef, MethodRef methodRef, IrExpression... arguments) {
        this.fieldRef = fieldRef;
        this.methodRef = methodRef;
        this.arguments = Arrays.stream(arguments).toList();
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrStaticCall(this);
    }
}
