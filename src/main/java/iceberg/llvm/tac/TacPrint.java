package iceberg.llvm.tac;

public class TacPrint implements TAC {

    public final TacTyped argument;

    public TacPrint(TacTyped argument) {
        this.argument = argument;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacPrint(this);
    }

    @Override
    public String toString() {
        return "print " + argument;
    }
}
