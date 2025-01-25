package iceberg.jvm.ir;

import org.jetbrains.annotations.Nullable;

public class IrIfStatement implements IR {

    public final IrExpression condition;
    public final IR thenStatement;
    public final @Nullable IR elseStatement;

    public IrIfStatement(
        IrExpression condition,
        IR thenStatement,
        @Nullable IR elseStatement
    ) {
        this.condition = condition;
        this.thenStatement = thenStatement;
        this.elseStatement = elseStatement;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrIfStatement(this);
    }
}
