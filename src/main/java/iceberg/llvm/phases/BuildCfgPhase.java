package iceberg.llvm.phases;

import iceberg.llvm.BasicBlock;
import iceberg.llvm.FunctionCfg;
import iceberg.llvm.FunctionTac;
import iceberg.llvm.tac.TacJump;
import iceberg.llvm.tac.TacJumpConditional;

import java.util.HashMap;
import java.util.Map;

public class BuildCfgPhase {

    private final FunctionTac function;

    public BuildCfgPhase(FunctionTac function) {
        this.function = function;
    }

    private final Map<Integer, BasicBlock> bbs = new HashMap<>();

    public FunctionCfg execute() {
        dfs(0);

        for (var basicBlock : bbs.values()) {
            enrich(basicBlock);
        }

        return new FunctionCfg(function, bbs);
    }

    private void dfs(int start) {
        if (bbs.containsKey(start)) {
            return;
        }

        var bb = bbs.computeIfAbsent(start, __ -> new BasicBlock(synth()));

        int offset = start;
        while (offset < function.tac.size()) {
            var tac = function.tac.get(offset);
            bb.tac.add(tac);

            if (tac instanceof TacJump jump) {
                dfs(jump.gotoOffset);
                return;
            }

            if (tac instanceof TacJumpConditional jumpConditional) {
                dfs(jumpConditional.thenOffset);
                dfs(jumpConditional.elseOffset);
                return;
            }

            offset++;
        }
    }

    private void enrich(BasicBlock currentBlock) {
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
