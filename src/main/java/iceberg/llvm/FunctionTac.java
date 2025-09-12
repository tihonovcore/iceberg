package iceberg.llvm;

import iceberg.ir.IrFunction;
import iceberg.llvm.tac.TAC;

import java.util.LinkedList;
import java.util.List;

public class FunctionTac {

    public final IrFunction irFunction;
    public final List<TAC> tac = new LinkedList<>();

    public FunctionTac(IrFunction irFunction) {
        this.irFunction = irFunction;
    }
}
