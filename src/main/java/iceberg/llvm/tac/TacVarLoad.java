package iceberg.llvm.tac;

public class TacVarLoad implements TAC {

    public final TacVariable target;
    public final TacVariable memory;

    public TacVarLoad(TacVariable target, TacVariable memory) {
        this.target = target;
        this.memory = memory;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacVarLoad(this);
    }

    @Override
    public String toString() {
        return target + " = load " + memory.type + ", " + memory.type + "* " + memory;
    }
}
