package iceberg.llvm.phases;

import iceberg.llvm.BasicBlock;
import iceberg.llvm.FunctionCfg;
import iceberg.llvm.FunctionTac;
import iceberg.llvm.tac.TacJump;
import iceberg.llvm.tac.TacJumpConditional;

import java.util.SortedMap;
import java.util.TreeMap;

public class BuildCfgPhase {

    private final FunctionTac function;

    public BuildCfgPhase(FunctionTac function) {
        this.function = function;
    }

    private final SortedMap<Integer, BasicBlock> bbs = new TreeMap<>();

    public FunctionCfg execute() {
        //TODO: normalize returns - для упрощения dataflow-анализа
        // лучше иметь один блок с return

        createBasicBlocks(0);
        fillBasicBlocksWithInstructions();

        for (var basicBlock : bbs.values()) {
            buildEdgesFrom(basicBlock);
        }

        return new FunctionCfg(function, bbs);
    }

    private void createBasicBlocks(int start) {
        if (bbs.containsKey(start)) {
            return;
        } else {
            bbs.put(start, new BasicBlock(synth()));
        }

        int offset = start;
        while (offset < function.tac.size()) {
            var tac = function.tac.get(offset);

            if (tac instanceof TacJump jump) {
                createBasicBlocks(jump.gotoOffset);
                return;
            }

            if (tac instanceof TacJumpConditional jumpConditional) {
                createBasicBlocks(jumpConditional.thenOffset);
                createBasicBlocks(jumpConditional.elseOffset);
                return;
            }

            offset++;
        }
    }

    private void fillBasicBlocksWithInstructions() {
        BasicBlock currentBlock = bbs.get(0);
        for (int i = 0; i < function.tac.size(); i++) {
            if (bbs.containsKey(i)) {
                currentBlock = bbs.get(i);
            }

            var tac = function.tac.get(i);
            currentBlock.tac.add(tac);
        }
    }

    private void buildEdgesFrom(BasicBlock currentBlock) {
        currentBlock.tac.stream()
            .filter(TacJump.class::isInstance)
            .map(TacJump.class::cast)
            .forEach(jump -> {
                var next = bbs.get(jump.gotoOffset);

                currentBlock.next.add(next);
                next.prev.add(currentBlock);

                jump.gotoLabel = next.label;
            });

        currentBlock.tac.stream()
            .filter(TacJumpConditional.class::isInstance)
            .map(TacJumpConditional.class::cast)
            .forEach(jump -> {
                var nextThen = bbs.get(jump.thenOffset);
                var nextElse = bbs.get(jump.elseOffset);

                currentBlock.next.add(nextThen);
                nextThen.prev.add(currentBlock);

                currentBlock.next.add(nextElse);
                nextElse.prev.add(currentBlock);

                jump.thenLabel = nextThen.label;
                jump.elseLabel = nextElse.label;
            });
    }

    private int freeLabelIndex = 0;

    private String synth() {
        return "label_" + freeLabelIndex++;
    }
}
