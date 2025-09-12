package iceberg.llvm.tac;

public interface TacVisitor {

    void visitTacBinaryOperation(TacBinaryOperation tacBinaryOperation);
    void visitTacFunction(TacFunction tacFunction);
    void visitTacNumber(TacNumber tacNumber);
    void visitTacPrint(TacPrint tacPrint);
    void visitTacReturn(TacReturn tacReturn);
    void visitTacVariable(TacVariable tacVariable);
}
