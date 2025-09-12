package iceberg.llvm.tac;

import java.util.LinkedList;
import java.util.List;

public class TacFunction implements TAC {

    //todo: params?
    //todo: func name?
    public final List<TAC> tac = new LinkedList<>();

    @Override
    public void accept(TacVisitor visitor) {
        visitor.visitTacFunction(this);
    }
}
