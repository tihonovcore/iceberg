package iceberg.llvm.tac;

public interface TacVisitor {

    void visitTacBinaryOperation(TacBinaryOperation tacBinaryOperation);
    void visitTacCast(TacCast tacCast);
    void visitTacFunction(TacFunction tacFunction);
    void visitTacNumber(TacNumber tacNumber);
    void visitTacPrint(TacPrint tacPrint);
    void visitTacReturn(TacReturn tacReturn);
    void visitTacUnaryOperation(TacUnaryOperation tacUnaryOperation);
    void visitTacVariable(TacVariable tacVariable);
}
