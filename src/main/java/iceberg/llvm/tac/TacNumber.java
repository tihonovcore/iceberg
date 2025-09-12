package iceberg.llvm.tac;

import iceberg.ir.IcebergType;

public class TacNumber extends TacTyped {

    public final long value;

    public TacNumber(long value, IcebergType type) {
        super(type);
        this.value = value;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacNumber(this);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
