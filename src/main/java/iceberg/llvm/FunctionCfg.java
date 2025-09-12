package iceberg.llvm;

import iceberg.ir.IrFunction;
import iceberg.llvm.tac.TAC;

import java.util.List;
import java.util.Map;

public class FunctionCfg {

    public final IrFunction irFunction;
    public final List<TAC> tac;
    public final Map<Integer, BasicBlock> bbs;

    public FunctionCfg(
        FunctionTac functionTac,
        Map<Integer, BasicBlock> bbs
    ) {
        this.irFunction = functionTac.irFunction;
        this.tac = functionTac.tac;
        this.bbs = bbs;
    }
}
