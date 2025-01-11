package iceberg.jvm.phases;

import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;
import org.antlr.v4.runtime.tree.TerminalNode;

public class FillConstantPoolPhase implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        file.accept(new IcebergBaseVisitor<>() {
            @Override
            public Object visitTerminal(TerminalNode node) {
                switch (node.getSymbol().getType()) {
                    case IcebergLexer.NUMBER -> {
                        long value = Long.parseLong(node.getText());
                        if (value < Integer.MIN_VALUE || Integer.MAX_VALUE < value) {
                            unit.constantPool.addLong(value);
                        } else if (value < Short.MIN_VALUE || Short.MAX_VALUE < value) {
                            unit.constantPool.addInteger((int) value);
                        }
                    }
                }

                return super.visitTerminal(node);
            }
        });
    }
}
