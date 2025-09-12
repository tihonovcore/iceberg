package iceberg.llvm;

import iceberg.llvm.tac.TAC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BasicBlock {

    public final String label;
    public final List<TAC> tac = new ArrayList<>();
    public final Collection<BasicBlock> next = new ArrayList<>();
    public final Collection<BasicBlock> prev = new ArrayList<>();

    public BasicBlock(String label) {
        this.label = label;
    }
}
