package iceberg.llvm;

import iceberg.ir.IrFunction;
import iceberg.llvm.tac.TAC;

import java.util.List;
import java.util.SortedMap;

public class FunctionCfg {

    public final IrFunction irFunction;
    public final List<TAC> tac;
    public final SortedMap<Integer, BasicBlock> bbs;

    public FunctionCfg(
        FunctionTac functionTac,
        SortedMap<Integer, BasicBlock> bbs
    ) {
        this.irFunction = functionTac.irFunction;
        this.tac = functionTac.tac;
        this.bbs = bbs;
    }
}
