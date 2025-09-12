package iceberg.llvm.phases;

import iceberg.llvm.BasicBlock;
import iceberg.llvm.tac.TacFunction;

public class BuildCfgPhase {

    private final TacFunction function;

    public BuildCfgPhase(TacFunction function) {
        this.function = function;
    }

    public BasicBlock execute() {
        var bb = new BasicBlock(synth());

        //TODO: build graph instead
        bb.tac.addAll(function.tac);

        return bb;
    }

    private int freeLabelIndex = 0;

    private String synth() {
        return "label_" + freeLabelIndex++;
    }
}
