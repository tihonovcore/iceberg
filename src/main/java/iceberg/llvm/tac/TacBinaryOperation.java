package iceberg.llvm.tac;

import iceberg.ir.IcebergBinaryOperator;

public class TacBinaryOperation implements TAC {

    public final TacVariable target;
    public final TacTyped left;
    public final TacTyped right;
    public final IcebergBinaryOperator operator;

    public TacBinaryOperation(
        TacVariable target,
        TacTyped left,
        TacTyped right,
        IcebergBinaryOperator operator
    ) {
        this.target = target;
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacBinaryOperation(this);
    }

    @Override
    public String toString() {
        return target + " = " + left + " " + operator + " " + right;
    }
}
