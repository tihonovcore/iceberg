package iceberg.jvm;

import antlr.IcebergParser;

import java.io.ByteArrayOutputStream;

public class Compiler {

    private ConstantPool constantPool = new ConstantPool();
    private ByteArrayOutputStream output;

    public byte[] compile(IcebergParser.FileContext file) {
        return codegen();
    }

    private byte[] codegen() {
        output = new ByteArrayOutputStream();

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

        return output.toByteArray();
    }

    private void magic() {
        output.write(0xCA);
        output.write(0xFE);
        output.write(0xBA);
        output.write(0xBE);
    }

    private void minorVersion() {
        output.write(0x00);
        output.write(0x00);
    }

    private void majorVersion() {
        output.write(0x00);
        output.write(0x3D);
    }

    private void constantPoolCount() {
        output.write(constantPool.count() >> 8);
        output.write(constantPool.count());
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
        output.write(flags >> 8);
        output.write(flags);
    }

    private void thisClass() {
        var thisClass = 19; //todo: find in constant pool
        output.write(thisClass >> 8);
        output.write(thisClass);
    }

    private void superClass() {
        var superClass = 2; //todo: find in constant pool
        output.write(superClass >> 8);
        output.write(superClass);
    }

    private void interfacesCount() {
        output.write(0);
        output.write(0);
    }

    private void interfaces() {

    }

    private void fieldsCount() {
        output.write(0);
        output.write(0);
    }

    private void fields() {

    }

    private void methodsCount() {
        var methodsCount = 2; //init + main
        output.write(methodsCount >> 8);
        output.write(methodsCount);
    }

    private void methods() {
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
            output.write(flags >> 8);
            output.write(flags);

            var nameIndex = 5; //todo: find in constant pool
            output.write(nameIndex >> 8);
            output.write(nameIndex);

            var descriptorIndex = 6; //todo: find in constant pool
            output.write(descriptorIndex >> 8);
            output.write(descriptorIndex);

            var attributesCount = 1;
            output.write(attributesCount >> 8);
            output.write(attributesCount);

            code : {
                var attributeNameIndex = 21;
                output.write(attributeNameIndex >> 8);
                output.write(attributeNameIndex);

                var attributeLength = 0x2F;
                output.write(attributeLength >> 24);
                output.write(attributeLength >> 16);
                output.write(attributeLength >> 8);
                output.write(attributeLength);

                var maxStack = 1;
                output.write(maxStack >> 8);
                output.write(maxStack);

                var maxLocals = 1;
                output.write(maxLocals >> 8);
                output.write(maxLocals);

                var codeLength = 5;
                output.write(codeLength >> 24);
                output.write(codeLength >> 16);
                output.write(codeLength >> 8);
                output.write(codeLength);

                output.write(OpCodes.ALOAD_0.value);
                output.write(OpCodes.INVOKESPECIAL.value);
                output.write(0); // Method java/lang/Object."<init>":()V
                output.write(1); // Method java/lang/Object."<init>":()V
                output.write(OpCodes.RETURN.value);

                var exceptionTableLength = 0;
                output.write(exceptionTableLength >> 8);
                output.write(exceptionTableLength);

                attributesCount = 2;
                output.write(attributesCount >> 8);
                output.write(attributesCount);

                //LineNumberTable
                attributeNameIndex = 22;
                output.write(attributeNameIndex >> 8);
                output.write(attributeNameIndex);

                attributeLength = 6;
                output.write(attributeLength >> 24);
                output.write(attributeLength >> 16);
                output.write(attributeLength >> 8);
                output.write(attributeLength);

                var lineNumberTableLength = 1;
                output.write(lineNumberTableLength >> 8);
                output.write(lineNumberTableLength);

                var startPc = 0;
                output.write(startPc >> 8);
                output.write(startPc);

                var lineNumber = 1;
                output.write(lineNumber >> 8);
                output.write(lineNumber);

                //LocalVariableTable
                attributeNameIndex = 23;
                output.write(attributeNameIndex >> 8);
                output.write(attributeNameIndex);

                attributeLength = 0x0C;
                output.write(attributeLength >> 24);
                output.write(attributeLength >> 16);
                output.write(attributeLength >> 8);
                output.write(attributeLength);

                lineNumberTableLength = 1;
                output.write(lineNumberTableLength >> 8);
                output.write(lineNumberTableLength);

                startPc = 0;
                output.write(startPc >> 8);
                output.write(startPc);

                var length = 5;
                output.write(length >> 8);
                output.write(length);

                nameIndex = 0x18; //this
                output.write(nameIndex >> 8);
                output.write(nameIndex);

                descriptorIndex = 0x19; //LFoo
                output.write(descriptorIndex >> 8);
                output.write(descriptorIndex);

                var index = 0;
                output.write(index >> 8);
                output.write(index);
            }
        }

        main : {
            var flags = AccessFlags.ACC_PUBLIC.value | AccessFlags.ACC_STATIC.value;
            output.write(flags >> 8);
            output.write(flags);

            var nameIndex = 26; //todo: find in constant pool
            output.write(nameIndex >> 8);
            output.write(nameIndex);

            var descriptorIndex = 27; //todo: find in constant pool
            output.write(descriptorIndex >> 8);
            output.write(descriptorIndex);

            var attributesCount = 1;
            output.write(attributesCount >> 8);
            output.write(attributesCount);

            code : {
                var attributeNameIndex = 21;
                output.write(attributeNameIndex >> 8);
                output.write(attributeNameIndex);

                var attributeLength = 0x38;
                output.write(attributeLength >> 24);
                output.write(attributeLength >> 16);
                output.write(attributeLength >> 8);
                output.write(attributeLength);

                var maxStack = 2;
                output.write(maxStack >> 8);
                output.write(maxStack);

                var maxLocals = 1;
                output.write(maxLocals >> 8);
                output.write(maxLocals);

                var codeLength = 10;
                output.write(codeLength >> 24);
                output.write(codeLength >> 16);
                output.write(codeLength >> 8);
                output.write(codeLength);

                output.write(OpCodes.GETSTATIC.value);
                output.write(0); // Field java/lang/System.out:Ljava/io/PrintStream;
                output.write(7); // Field java/lang/System.out:Ljava/io/PrintStream;
                output.write(OpCodes.SIPUSH.value);
                output.write(499 >> 8);
                output.write(499);
                output.write(OpCodes.INVOKEVIRTUAL.value);
                output.write(0);  // Method java/io/PrintStream.println:(I)V
                output.write(13); // Method java/io/PrintStream.println:(I)V
                output.write(OpCodes.RETURN.value);

                var exceptionTableLength = 0;
                output.write(exceptionTableLength >> 8);
                output.write(exceptionTableLength);

                attributesCount = 2;
                output.write(attributesCount >> 8);
                output.write(attributesCount);

                //LineNumberTable
                attributeNameIndex = 22;
                output.write(attributeNameIndex >> 8);
                output.write(attributeNameIndex);

                attributeLength = 10;
                output.write(attributeLength >> 24);
                output.write(attributeLength >> 16);
                output.write(attributeLength >> 8);
                output.write(attributeLength);

                var lineNumberTableLength = 2;
                output.write(lineNumberTableLength >> 8);
                output.write(lineNumberTableLength);

                var startPc = 0;
                output.write(startPc >> 8);
                output.write(startPc);

                var lineNumber = 3;
                output.write(lineNumber >> 8);
                output.write(lineNumber);

                startPc = 9;
                output.write(startPc >> 8);
                output.write(startPc);

                lineNumber = 4;
                output.write(lineNumber >> 8);
                output.write(lineNumber);

                //LocalVariableTable
                attributeNameIndex = 23;
                output.write(attributeNameIndex >> 8);
                output.write(attributeNameIndex);

                attributeLength = 0x0C;
                output.write(attributeLength >> 24);
                output.write(attributeLength >> 16);
                output.write(attributeLength >> 8);
                output.write(attributeLength);

                lineNumberTableLength = 1;
                output.write(lineNumberTableLength >> 8);
                output.write(lineNumberTableLength);

                startPc = 0;
                output.write(startPc >> 8);
                output.write(startPc);

                var length = 10;
                output.write(length >> 8);
                output.write(length);

                nameIndex = 0x1C; //args
                output.write(nameIndex >> 8);
                output.write(nameIndex);

                descriptorIndex = 0x1D; //[String
                output.write(descriptorIndex >> 8);
                output.write(descriptorIndex);

                var index = 0;
                output.write(index >> 8);
                output.write(index);
            }
        }
    }

    private void attributesCount() {
        var attributeCount = 1;
        output.write(attributeCount >> 8);
        output.write(attributeCount);
    }

    private void attributes() {
        sourceFile : {
            var nameIndex = 30; //todo: find in constant pool
            output.write(nameIndex >> 8);
            output.write(nameIndex);

            final var length = 2; //always 2
            output.write(length >> 24);
            output.write(length >> 16);
            output.write(length >> 8);
            output.write(length);

            var sourceFileIndex = 31; //todo: find in constant pool
            output.write(sourceFileIndex >> 8);
            output.write(sourceFileIndex);
        }
    }
}
