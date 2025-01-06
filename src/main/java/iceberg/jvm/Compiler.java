package iceberg.jvm;

import antlr.IcebergParser;

public class Compiler {

    private ConstantPool constantPool = new ConstantPool();
    private ByteArray output;

    public byte[] compile(IcebergParser.FileContext file) {
        return codegen(file);
    }

    private byte[] codegen(IcebergParser.FileContext file) {
        output = new ByteArray();

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
        output.writeU2(constantPool.count());
    }

    private void constantPool() {
        for (var constant : constantPool) {
            output.writeBytes(ConstantPool.ConstantToBytes.toBytes(constant));
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
        var thisClass = 19; //todo: find in constant pool
        output.writeU2(thisClass);
    }

    private void superClass() {
        var superClass = 2; //todo: find in constant pool
        output.writeU2(superClass);
    }

    private void interfacesCount() {
        var interfacesCount = 0;
        output.writeU2(interfacesCount);
    }

    private void interfaces() {

    }

    private void fieldsCount() {
        var fieldsCount = 0;
        output.writeU2(fieldsCount);
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

        enum OpCodes {
            ALOAD_0(0x2A),
            RETURN(0xB1),
            GETSTATIC(0xB2),
            INVOKEVIRTUAL(0xB6),
            INVOKESPECIAL(0xB7),
            BIPUSH(0x10),
            SIPUSH(0x11),
            ;

            OpCodes(int value) {
                this.value = value;
            }

            final int value;
        }

        init : {
            var flags = AccessFlags.ACC_PUBLIC.value;
            output.writeU2(flags);

            var nameIndex = 5; //todo: find in constant pool
            output.writeU2(nameIndex);

            var descriptorIndex = 6; //todo: find in constant pool
            output.writeU2(descriptorIndex);

            var attributesCount = 1;
            output.writeU2(attributesCount);

            code : {
                var attributeNameIndex = 21;
                output.writeU2(attributeNameIndex);

                var attributeLength = 0x2F;
                output.writeU4(attributeLength);

                var maxStack = 1;
                output.writeU2(maxStack);

                var maxLocals = 1;
                output.writeU2(maxLocals);

                var codeLength = 5;
                output.writeU4(codeLength);

                output.writeU1(OpCodes.ALOAD_0.value);
                output.writeU1(OpCodes.INVOKESPECIAL.value);
                output.writeU2(0x0001); // Method java/lang/Object."<init>":()V
                output.writeU1(OpCodes.RETURN.value);

                var exceptionTableLength = 0;
                output.writeU2(exceptionTableLength);

                attributesCount = 2;
                output.writeU2(attributesCount);

                //LineNumberTable
                attributeNameIndex = 22;
                output.writeU2(attributeNameIndex);

                attributeLength = 6;
                output.writeU4(attributeLength);

                var lineNumberTableLength = 1;
                output.writeU2(lineNumberTableLength);

                var startPc = 0;
                output.writeU2(startPc);

                var lineNumber = 1;
                output.writeU2(lineNumber);

                //LocalVariableTable
                attributeNameIndex = 23;
                output.writeU2(attributeNameIndex);

                attributeLength = 0x0C;
                output.writeU4(attributeLength);

                lineNumberTableLength = 1;
                output.writeU2(lineNumberTableLength);

                startPc = 0;
                output.writeU2(startPc);

                var length = 5;
                output.writeU2(length);

                nameIndex = 0x18; //this
                output.writeU2(nameIndex);

                descriptorIndex = 0x19; //LFoo
                output.writeU2(descriptorIndex);

                var index = 0;
                output.writeU2(index);
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

                var codeAttributeLengthIndex = output.length();
                var attributeLength = 0x0000;
                output.writeU4(attributeLength);
                var codeAttributeStartIndex = output.length();

                var maxStack = 2;
                output.writeU2(maxStack);

                var maxLocals = 1;
                output.writeU2(maxLocals);

                var codeLengthIndex = output.length();
                var codeLength = 0x0000;
                output.writeU4(codeLength);

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
                    } else {
                        throw new IllegalStateException("not implemented");
                    }

                    output.writeU1(OpCodes.INVOKEVIRTUAL.value);
                    output.writeU2(0x000D); // Method java/io/PrintStream.println:(I)V
                }
                output.writeU1(OpCodes.RETURN.value);

                //fill codeLength
                output.putU4(codeLengthIndex, output.length() - codeStartIndex);

                var exceptionTableLength = 0;
                output.writeU2(exceptionTableLength);

                attributesCount = 0;
                output.writeU2(attributesCount);

//                //LineNumberTable
//                attributeNameIndex = 22;
//                output.writeU2(attributeNameIndex);
//
//                attributeLength = 10;
//                output.writeU4(attributeLength);
//
//                var lineNumberTableLength = 2;
//                output.writeU2(lineNumberTableLength);
//
//                var startPc = 0;
//                output.writeU2(startPc);
//
//                var lineNumber = 3;
//                output.writeU2(lineNumber);
//
//                startPc = 9;
//                output.writeU2(startPc);
//
//                lineNumber = 4;
//                output.writeU2(lineNumber);

//                //LocalVariableTable
//                attributeNameIndex = 23;
//                output.writeU2(attributeNameIndex);
//
//                attributeLength = 0x0C;
//                output.writeU4(attributeLength);
//
//                lineNumberTableLength = 1;
//                output.writeU2(lineNumberTableLength);
//
//                startPc = 0;
//                output.writeU2(startPc);
//
//                var length = 10;
//                output.writeU2(length);
//
//                nameIndex = 0x1C; //args
//                output.writeU2(nameIndex);
//
//                descriptorIndex = 0x1D; //[String
//                output.writeU2(descriptorIndex);
//
//                var index = 0;
//                output.writeU2(index);

                //fill codeAttributeLength
                output.putU4(codeAttributeLengthIndex, output.length() - codeAttributeStartIndex);
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
