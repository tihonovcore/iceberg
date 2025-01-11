package iceberg.jvm.ir;

import iceberg.jvm.cp.StringInfo;

public class IrString extends IrExpression {

    public final StringInfo value;

    public IrString(StringInfo value) {
        super(IcebergType.string);
        this.value = value;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrString(this);
    }
}
