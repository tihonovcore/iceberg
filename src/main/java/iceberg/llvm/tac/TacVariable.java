package iceberg.llvm.tac;

import iceberg.ir.IcebergType;

public class TacVariable extends TacTyped {

    public final String name;

    public TacVariable(String name, IcebergType type) {
        super(type);
        this.name = name;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacVariable(this);
    }
}
