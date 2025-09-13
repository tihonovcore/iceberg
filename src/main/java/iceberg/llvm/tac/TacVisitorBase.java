package iceberg.llvm.tac;

public abstract class TacVisitorBase implements TacVisitor {

    public void visitTacBinaryOperation(TacBinaryOperation tacBinaryOperation) {
        tacBinaryOperation.target.accept(this);
        tacBinaryOperation.left.accept(this);
        tacBinaryOperation.right.accept(this);
    }
    
    public void visitTacCast(TacCast tacCast) {
        tacCast.target.accept(this);
        tacCast.argument.accept(this);
    }
    
    public void visitTacJump(TacJump tacJump) {
        //no children
    }
    
    public void visitTacJumpConditional(TacJumpConditional tacJumpConditional) {
        //no children
    }
    
    public void visitTacNumber(TacNumber tacNumber) {
        //no children
    }
    
    public void visitTacPrint(TacPrint tacPrint) {
        tacPrint.argument.accept(this);
    }
    
    public void visitTacReturn(TacReturn tacReturn) {
        if (tacReturn.argument != null) {
            tacReturn.argument.accept(this);
        }
    }
    
    public void visitTacUnaryOperation(TacUnaryOperation tacUnaryOperation) {
        tacUnaryOperation.target.accept(this);
        tacUnaryOperation.argument.accept(this);
    }
    
    public void visitTacVariable(TacVariable tacVariable) {
        //no children
    }
    
    public void visitTacVarAllocate(TacVarAllocate tacVarAllocate) {
        tacVarAllocate.target.accept(this);
    }
    
    public void visitTacVarLoad(TacVarLoad tacVarLoad) {
        tacVarLoad.target.accept(this);
        tacVarLoad.memory.accept(this);
    }
    
    public void visitTacVarStore(TacVarStore tacVarStore) {
        tacVarStore.target.accept(this);
        tacVarStore.argument.accept(this);
    }
}
