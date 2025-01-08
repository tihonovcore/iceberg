package iceberg.jvm;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.cp.ConstantToBytes;

import java.util.Collection;

public class CodeGenerator {

    private final CompilationUnit compilationUnit;
    private final ByteArray output = new ByteArray();

    public CodeGenerator(CompilationUnit unit) {
        this.compilationUnit = unit;
    }

    public static void codegen(
        Collection<CompilationUnit> units,
        @Deprecated IcebergParser.FileContext file
    ) {
        units.forEach(unit -> {
            var generator = new CodeGenerator(unit);
            unit.bytes = generator.codegen(file);
        });
    }

    public byte[] codegen(IcebergParser.FileContext file) {
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
        methods(file);
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
        var methodsCount = 2; //init + main
        output.writeU2(methodsCount);
    }

    private void methods(IcebergParser.FileContext file) {
        enum AccessFlags {

            ACC_PUBLIC(0x0001),
            ACC_STATIC(0x0008),
            ;

            AccessFlags(int value) {
                this.value = value;
            }

            final int value;
        }

        init : {
            var method = compilationUnit.methods.get(0);

            var flags = AccessFlags.ACC_PUBLIC.value;
            output.writeU2(flags);

            output.writeU2(compilationUnit.constantPool.indexOf(method.name));
            output.writeU2(compilationUnit.constantPool.indexOf(method.descriptor));

            output.writeU2(method.attributes.size());

            code : {
                var attribute = (CompilationUnit.CodeAttribute) method.attributes.get(0);

                output.writeU2(compilationUnit.constantPool.indexOf(attribute.attributeName));

                var codeAttributeLength = output.lateInitU4();

                output.writeU2(attribute.maxStack);
                output.writeU2(attribute.maxLocals);
                output.writeU4(attribute.code.length);
                output.writeBytes(attribute.code);

                output.writeU2(attribute.exceptionTable.size());
                output.writeU2(attribute.attributes.size());

                codeAttributeLength.init();
            }
        }

        main : {
            var flags = AccessFlags.ACC_PUBLIC.value | AccessFlags.ACC_STATIC.value;
            output.writeU2(flags);

            var nameIndex = 26; //todo: find in constant pool
            output.writeU2(nameIndex);

            var descriptorIndex = 27; //todo: find in constant pool
            output.writeU2(descriptorIndex);

            var attributesCount = 1;
            output.writeU2(attributesCount);

            code : {
                var attributeNameIndex = 21;
                output.writeU2(attributeNameIndex);

                var codeAttributeLength = output.lateInitU4();

                var maxStack = 2;
                output.writeU2(maxStack);

                var maxLocals = 1;
                output.writeU2(maxLocals);

                var codeLength = output.lateInitU4();

                var codeStartIndex = output.length();
                for (var print : file.printStatement()) {
                    output.writeU1(OpCodes.GETSTATIC.value);
                    output.writeU2(0x0007); // Field java/lang/System.out:Ljava/io/PrintStream;

                    var value = Integer.parseInt(print.expression().getText());
                    if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
                        output.writeU1(OpCodes.BIPUSH.value);
                        output.writeU1(value);
                    } else if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE) {
                        output.writeU1(OpCodes.SIPUSH.value);
                        output.writeU2(value);
                    } else if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
                        var indexInPool = compilationUnit.constantPool.findInteger(value);
                        if (Byte.MIN_VALUE <= indexInPool && indexInPool <= Byte.MAX_VALUE) {
                            output.writeU1(OpCodes.LDC.value);
                            output.writeU1(indexInPool);
                        } else {
                            output.writeU1(OpCodes.LDC_W.value);
                            output.writeU2(indexInPool);
                        }
                    } else {
                        throw new IllegalStateException("not implemented");
                    }

                    output.writeU1(OpCodes.INVOKEVIRTUAL.value);
                    output.writeU2(0x000D); // Method java/io/PrintStream.println:(I)V
                }
                output.writeU1(OpCodes.RETURN.value);

                //fill codeLength
                codeLength.init();

                var exceptionTableLength = 0;
                output.writeU2(exceptionTableLength);

                attributesCount = 0;
                output.writeU2(attributesCount);

                //fill codeAttributeLength
                codeAttributeLength.init();
            }
        }
    }

    private void attributesCount() {
        var attributeCount = 1;
        output.writeU2(attributeCount);
    }

    private void attributes() {
        sourceFile : {
            var nameIndex = 30; //todo: find in constant pool
            output.writeU2(nameIndex);

            final var length = 2; //always 2
            output.writeU4(length);

            var sourceFileIndex = 31; //todo: find in constant pool
            output.writeU2(sourceFileIndex);
        }
    }
}
