package iceberg.llvm.tac;

public class TacJump implements TAC {

    public int gotoOffset = -1;
    public String gotoLabel;

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacJump(this);
    }

    @Override
    public String toString() {
        return "goto " + gotoOffset;
    }
}
