package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.ir.*;

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
        attribute.maxStack = 2; //TODO: how to evaluate?
        attribute.maxLocals = 1; //TODO: how to evaluate?

        var system = unit.constantPool.computeKlass(
            unit.constantPool.computeUtf8("java/lang/System")
        );
        var out = unit.constantPool.computeNameAndType(
            unit.constantPool.computeUtf8("out"),
            unit.constantPool.computeUtf8("Ljava/io/PrintStream;")
        );
        var field = unit.constantPool.computeFieldRef(system, out);

        var printStream = unit.constantPool.computeKlass(
            unit.constantPool.computeUtf8("java/io/PrintStream")
        );
        var println = unit.constantPool.computeNameAndType(
            unit.constantPool.computeUtf8("println"),
            unit.constantPool.computeUtf8("(I)V")
        );
        var method = unit.constantPool.computeMethodRef(printStream, println);

        var irBody = new IrBody();
        for (var statement : file.printStatement()) {
            var value = Integer.parseInt(statement.expression().getText());
            var constant = new IrNumber(value);

            var irStaticCall = new IrStaticCall(field, method, constant);

            irBody.statements.add(irStaticCall);
        }
        irBody.statements.add(new IrReturn());

        attribute.body = irBody;

        return attribute;
    }
}
