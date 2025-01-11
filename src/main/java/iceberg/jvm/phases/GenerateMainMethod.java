package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.ir.*;

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

        var system = unit.constantPool.computeKlass(
            unit.constantPool.computeUtf8("java/lang/System")
        );
        var out = unit.constantPool.computeNameAndType(
            unit.constantPool.computeUtf8("out"),
            unit.constantPool.computeUtf8("Ljava/io/PrintStream;")
        );
        var field = unit.constantPool.computeFieldRef(system, out);

        var irBody = new IrBody();
        for (var statement : file.printStatement()) {
            var value = Long.parseLong(statement.expression().getText());
            var constant = new IrNumber(value);

            var printStream = unit.constantPool.computeKlass(
                unit.constantPool.computeUtf8("java/io/PrintStream")
            );
            var println = unit.constantPool.computeNameAndType(
                unit.constantPool.computeUtf8("println"),
                unit.constantPool.computeUtf8(
                    Map.of(
                        IcebergType.i32, "(I)V",
                        IcebergType.i64, "(J)V",
                        IcebergType.bool, "(B)V",
                        IcebergType.string, "(Ljava/lang/String;)V"
                    ).get(constant.type)
                )
            );
            var method = unit.constantPool.computeMethodRef(printStream, println);

            var irStaticCall = new IrStaticCall(field, method, constant);

            irBody.statements.add(irStaticCall);
        }
        irBody.statements.add(new IrReturn());

        attribute.body = irBody;

        return attribute;
    }
}
