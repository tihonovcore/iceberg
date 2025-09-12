package iceberg.llvm.tac;

public class TacJumpConditional implements TAC {

    public final TacTyped condition;
    public int thenOffset = -1;
    public int elseOffset = -1;
    public String thenLabel;
    public String elseLabel;

    public TacJumpConditional(TacTyped condition) {
        this.condition = condition;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacJumpConditional(this);
    }

    @Override
    public String toString() {
        return "if " + condition + " goto " + thenOffset + " else goto " + elseOffset;
    }
}
