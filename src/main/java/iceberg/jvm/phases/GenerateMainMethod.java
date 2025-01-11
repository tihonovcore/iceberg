package iceberg.jvm.phases;

import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergLexer;
import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.ir.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Map;

public class GenerateMainMethod implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        var init = new CompilationUnit.Method();
        init.flags
            = CompilationUnit.Method.AccessFlags.ACC_PUBLIC.value
            | CompilationUnit.Method.AccessFlags.ACC_STATIC.value;
        init.name = unit.constantPool.computeUtf8("main");
        init.descriptor = unit.constantPool.computeUtf8("([Ljava/lang/String;)V");

        init.attributes.add(createCodeAttribute(file, unit));

        unit.methods.add(init);
    }

    private CompilationUnit.CodeAttribute createCodeAttribute(
        IcebergParser.FileContext file, CompilationUnit unit
    ) {
        var attribute = new CompilationUnit.CodeAttribute();
        attribute.attributeName = unit.constantPool.computeUtf8("Code");
        attribute.maxStack = 100; //TODO: how to evaluate?
        attribute.maxLocals = 100; //TODO: how to evaluate?

        attribute.body = (IrBody) file.accept(new IcebergBaseVisitor<IR>() {

            @Override
            public IR visitFile(IcebergParser.FileContext ctx) {
                var irBody = new IrBody();
                for (var statement : ctx.printStatement()) {
                    irBody.statements.add(statement.accept(this));
                }
                irBody.statements.add(new IrReturn());

                return irBody;
            }

            @Override
            public IR visitPrintStatement(IcebergParser.PrintStatementContext ctx) {
                var system = unit.constantPool.computeKlass(
                    unit.constantPool.computeUtf8("java/lang/System")
                );
                var out = unit.constantPool.computeNameAndType(
                    unit.constantPool.computeUtf8("out"),
                    unit.constantPool.computeUtf8("Ljava/io/PrintStream;")
                );
                var field = unit.constantPool.computeFieldRef(system, out);

                var argument = (IrExpression) ctx.expression().accept(this);
                var printStream = unit.constantPool.computeKlass(
                    unit.constantPool.computeUtf8("java/io/PrintStream")
                );
                var println = unit.constantPool.computeNameAndType(
                    unit.constantPool.computeUtf8("println"),
                    unit.constantPool.computeUtf8(
                        Map.of(
                            IcebergType.i32, "(I)V",
                            IcebergType.i64, "(J)V",
                            IcebergType.bool, "(Z)V",
                            IcebergType.string, "(Ljava/lang/String;)V"
                        ).get(argument.type)
                    )
                );
                var method = unit.constantPool.computeMethodRef(printStream, println);
                return new IrStaticCall(field, method, argument);
            }

            @Override
            public IR visitTerminal(TerminalNode node) {
                return switch (node.getSymbol().getType()) {
                    case IcebergLexer.NUMBER -> new IrNumber(Long.parseLong(node.getText()));
                    case IcebergLexer.FALSE -> new IrBool(false);
                    case IcebergLexer.TRUE -> new IrBool(true);
                    default -> null;
                };
            }

            @Override
            protected IR aggregateResult(IR aggregate, IR nextResult) {
                return nextResult != null ? nextResult : aggregate;
            }
        });

        return attribute;
    }
}
