package iceberg.llvm.tac;

public class TacVarAllocate implements TAC {

    public final TacVariable target;

    public TacVarAllocate(TacVariable target) {
        this.target = target;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacVarAllocate(this);
    }

    @Override
    public String toString() {
        return target + " = alloca " + target.type;
    }
}
