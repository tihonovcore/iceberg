package iceberg.llvm.tac;

import iceberg.ir.IcebergUnaryOperator;

public class TacUnaryOperation implements TAC {

    public final TacVariable target;
    public final TacTyped argument;
    public final IcebergUnaryOperator operator;

    public TacUnaryOperation(
        TacVariable target,
        TacTyped argument,
        IcebergUnaryOperator operator
    ) {
        this.target = target;
        this.argument = argument;
        this.operator = operator;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacUnaryOperation(this);
    }
}
