package iceberg.llvm.tac;

public interface TacVisitor {

    void visitTacBinaryOperation(TacBinaryOperation tacBinaryOperation);
    void visitTacCast(TacCast tacCast);
    void visitTacJump(TacJump tacJump);
    void visitTacJumpConditional(TacJumpConditional tacJumpConditional);
    void visitTacNumber(TacNumber tacNumber);
    void visitTacPrint(TacPrint tacPrint);
    void visitTacReturn(TacReturn tacReturn);
    void visitTacUnaryOperation(TacUnaryOperation tacUnaryOperation);
    void visitTacVariable(TacVariable tacVariable);
    void visitTacVarAllocate(TacVarAllocate tacVarAllocate);
    void visitTacVarLoad(TacVarLoad tacVarLoad);
    void visitTacVarStore(TacVarStore tacVarStore);
}
