package iceberg.llvm.tac;

public interface TAC {

    void accept(TacVisitor visitor);
}
