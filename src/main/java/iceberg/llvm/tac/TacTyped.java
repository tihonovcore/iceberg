package iceberg.llvm.tac;

import iceberg.ir.IcebergType;

public abstract class TacTyped implements TAC {

    public final IcebergType type;

    protected TacTyped(IcebergType type) {
        this.type = type;
    }
}
