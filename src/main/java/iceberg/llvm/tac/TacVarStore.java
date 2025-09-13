package iceberg.llvm.tac;

public class TacVarStore implements TAC {

    public final TacVariable target;
    public final TacTyped argument;

    public TacVarStore(TacVariable target, TacTyped argument) {
        this.target = target;
        this.argument = argument;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacVarStore(this);
    }

    @Override
    public String toString() {
        return "store " + argument.type + " " + argument + ", " + target.type + "* " + target;
    }
}
