package iceberg.llvm.tac;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TacReturn implements TAC {

    public final @Nullable TacTyped argument;

    public TacReturn() {
        this.argument = null;
    }

    public TacReturn(@NotNull TacTyped argument) {
        this.argument = argument;
    }

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacReturn(this);
    }

    @Override
    public String toString() {
        return argument == null ? "return" : "return " + argument;
    }
}
