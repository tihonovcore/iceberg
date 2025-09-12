package iceberg.llvm.tac;

public class TacCast implements TAC {

    public final TacVariable target;
    public final TacTyped argument;

    public TacCast(TacVariable target, TacTyped argument) {
        this.target = target;
        this.argument = argument;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacCast(this);
    }

    @Override
    public String toString() {
        return target + " = cast " + argument;
    }
}
