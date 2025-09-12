package iceberg.llvm.phases;

import iceberg.llvm.BasicBlock;
import iceberg.llvm.FunctionCfg;
import iceberg.llvm.FunctionTac;

import java.util.Map;

public class BuildCfgPhase {

    private final FunctionTac function;

    public BuildCfgPhase(FunctionTac function) {
        this.function = function;
    }

    public FunctionCfg execute() {
        var bb = new BasicBlock(synth());

        //TODO: build graph instead
        bb.tac.addAll(function.tac);

        return new FunctionCfg(function, Map.of(0, bb));
    }

    private int freeLabelIndex = 0;

    private String synth() {
        return "label_" + freeLabelIndex++;
    }
}
