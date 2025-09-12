package iceberg.llvm;

import iceberg.llvm.tac.TAC;

import java.util.*;

public class BasicBlock {

    public final String label;
    public final List<TAC> tac = new ArrayList<>();
    public final Set<BasicBlock> next = new HashSet<>();
    public final Set<BasicBlock> prev = new HashSet<>();

    public BasicBlock(String label) {
        this.label = label;
    }
}
