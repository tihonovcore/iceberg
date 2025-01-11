package iceberg.jvm;

import iceberg.jvm.cp.ConstantToBytes;
import iceberg.jvm.ir.*;

import java.util.Collection;

public class CodeGenerator {

    private final CompilationUnit compilationUnit;
    private final ByteArray output = new ByteArray();

    public CodeGenerator(CompilationUnit unit) {
        this.compilationUnit = unit;
    }

    public static void codegen(Collection<CompilationUnit> units) {
        units.forEach(unit -> {
            var generator = new CodeGenerator(unit);
            unit.bytes = generator.codegen();
        });
    }

    public byte[] codegen() {
        magic();
        minorVersion();
        majorVersion();
        constantPoolCount();
        constantPool();
        accessFlags();
        thisClass();
        superClass();
        interfacesCount();
        interfaces();
        fieldsCount();
        fields();
        methodsCount();
        methods();
        attributesCount();
        attributes();

        return output.bytes();
    }

    private void magic() {
        output.writeU4(0xCAFEBABE);
    }

    private void minorVersion() {
        output.writeU2(0x0000);
    }

    private void majorVersion() {
        output.writeU2(0x003D);
    }

    private void constantPoolCount() {
        output.writeU2(compilationUnit.constantPool.count());
    }

    private void constantPool() {
        for (var constant : compilationUnit.constantPool) {
            output.writeBytes(ConstantToBytes.toBytes(constant));
        }
    }

    private void accessFlags() {
        enum AccessFlags {

            ACC_PUBLIC(0x0001),
            ACC_SUPER(0x0020),
            ;

            AccessFlags(int value) {
                this.value = value;
            }

            final int value;
        }

        var flags = AccessFlags.ACC_PUBLIC.value | AccessFlags.ACC_SUPER.value;
        output.writeU2(flags);
    }

    private void thisClass() {
        var thisRef = compilationUnit.thisRef;
        output.writeU2(compilationUnit.constantPool.indexOf(thisRef));
    }

    private void superClass() {
        var superRef = compilationUnit.superRef;
        output.writeU2(compilationUnit.constantPool.indexOf(superRef));
    }

    private void interfacesCount() {
        output.writeU2(compilationUnit.interfaces.size());
    }

    private void interfaces() {

    }

    private void fieldsCount() {
        output.writeU2(compilationUnit.fields.size());
    }

    private void fields() {

    }

    private void methodsCount() {
        output.writeU2(compilationUnit.methods.size());
    }

    private void methods() {
        for (var method : compilationUnit.methods) {
            output.writeU2(method.flags);
            output.writeU2(compilationUnit.constantPool.indexOf(method.name));
            output.writeU2(compilationUnit.constantPool.indexOf(method.descriptor));

            output.writeU2(method.attributes.size());

            for (var attribute : method.attributes) {
                output.writeU2(compilationUnit.constantPool.indexOf(attribute.attributeName));

                var codeAttributeLength = output.lateInitU4();

                output.writeU2(attribute.maxStack);
                output.writeU2(attribute.maxLocals);

                var codeLength = output.lateInitU4();
                attribute.body.accept(new IrVisitor() {
                    @Override
                    public void visitIrBody(IrBody irBody) {
                        irBody.statements.forEach(s -> s.accept(this));
                    }

                    @Override
                    public void visitIrSuperCall(IrSuperCall irSuperCall) {
                        output.writeU1(OpCodes.ALOAD_0.value);
                        output.writeU1(OpCodes.INVOKESPECIAL.value);
                        output.writeU2(compilationUnit.constantPool.indexOf(irSuperCall.methodRef));
                    }

                    @Override
                    public void visitIrReturn(IrReturn irReturn) {
                        output.writeU1(OpCodes.RETURN.value);
                    }

                    @Override
                    public void visitIrNumber(IrNumber irNumber) {
                        var value = irNumber.value;
                        if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
                            output.writeU1(OpCodes.BIPUSH.value);
                            output.writeU1((int) value);
                        } else if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE) {
                            output.writeU1(OpCodes.SIPUSH.value);
                            output.writeU2((int) value);
                        } else if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
                            var indexInPool = compilationUnit.constantPool.findInteger((int) value);
                            if (Byte.MIN_VALUE <= indexInPool && indexInPool <= Byte.MAX_VALUE) {
                                output.writeU1(OpCodes.LDC.value);
                                output.writeU1(indexInPool);
                            } else {
                                output.writeU1(OpCodes.LDC_W.value);
                                output.writeU2(indexInPool);
                            }
                        } else {
                            var indexInPool = compilationUnit.constantPool.findLong(value);
                            output.writeU1(OpCodes.LDC_W2.value);
                            output.writeU2(indexInPool);
                        }
                    }

                    @Override
                    public void visitIrBool(IrBool irBool) {
                        if (irBool.value) {
                            output.writeU1(OpCodes.ICONST_1.value);
                        } else {
                            output.writeU1(OpCodes.ICONST_0.value);
                        }
                    }

                    @Override
                    public void visitIrString(IrString irString) {
                        var indexInPool = compilationUnit.constantPool.indexOf(irString.value);
                        if (Byte.MIN_VALUE <= indexInPool && indexInPool <= Byte.MAX_VALUE) {
                            output.writeU1(OpCodes.LDC.value);
                            output.writeU1(indexInPool);
                        } else {
                            output.writeU1(OpCodes.LDC_W.value);
                            output.writeU2(indexInPool);
                        }
                    }

                    @Override
                    public void visitIrStaticCall(IrStaticCall irStaticCall) {
                        output.writeU1(OpCodes.GETSTATIC.value);
                        output.writeU2(compilationUnit.constantPool.indexOf(irStaticCall.fieldRef));

                        irStaticCall.arguments.forEach(e -> e.accept(this));

                        output.writeU1(OpCodes.INVOKEVIRTUAL.value);
                        output.writeU2(compilationUnit.constantPool.indexOf(irStaticCall.methodRef));
                    }
                });
                codeLength.init();

                output.writeU2(attribute.exceptionTable.size());
                output.writeU2(attribute.attributes.size());

                codeAttributeLength.init();
            }
        }
    }

    private void attributesCount() {
        output.writeU2(compilationUnit.attributes.size());
    }

    private void attributes() {
        for (var attribute : compilationUnit.attributes) {
            output.writeU2(compilationUnit.constantPool.indexOf(attribute.attributeName));
            final var length = 2; //always 2
            output.writeU4(length);
            output.writeU2(compilationUnit.constantPool.indexOf(attribute.sourceFileName));
        }
    }
}
