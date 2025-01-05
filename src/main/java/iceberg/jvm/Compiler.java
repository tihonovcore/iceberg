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
        output.write(constantPool.count() & 0xFF00);
        output.write(constantPool.count() & 0x00FF);
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
        output.write(flags & 0xFF00);
        output.write(flags & 0x00FF);
    }

    private void thisClass() {
        var thisClass = 19; //todo: find in constant pool
        output.write(thisClass & 0xFF00);
        output.write(thisClass & 0x00FF);
    }

    private void superClass() {
        var superClass = 2; //todo: find in constant pool
        output.write(superClass & 0xFF00);
        output.write(superClass & 0x00FF);
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
        output.write(methodsCount & 0xFF00);
        output.write(methodsCount & 0x00FF);
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
            output.write(flags & 0xFF00);
            output.write(flags & 0x00FF);

            var nameIndex = 5; //todo: find in constant pool
            output.write(nameIndex & 0xFF00);
            output.write(nameIndex & 0x00FF);

            var descriptorIndex = 6; //todo: find in constant pool
            output.write(descriptorIndex & 0xFF00);
            output.write(descriptorIndex & 0x00FF);

            var attributesCount = 1;
            output.write(attributesCount & 0xFF00);
            output.write(attributesCount & 0x00FF);

            code : {
                var attributeNameIndex = 21;
                output.write(attributeNameIndex & 0xFF00);
                output.write(attributeNameIndex & 0x00FF);

                var attributeLength = 0x2F;
                output.write(attributeLength & 0xFF000000);
                output.write(attributeLength & 0x00FF0000);
                output.write(attributeLength & 0x0000FF00);
                output.write(attributeLength & 0x000000FF);

                var maxStack = 1;
                output.write(maxStack & 0xFF00);
                output.write(maxStack & 0x00FF);

                var maxLocals = 1;
                output.write(maxLocals & 0xFF00);
                output.write(maxLocals & 0x00FF);

                var codeLength = 5;
                output.write(codeLength & 0xFF000000);
                output.write(codeLength & 0x00FF0000);
                output.write(codeLength & 0x0000FF00);
                output.write(codeLength & 0x000000FF);

                output.write(OpCodes.ALOAD_0.value);
                output.write(OpCodes.INVOKESPECIAL.value);
                output.write(0); // Method java/lang/Object."<init>":()V
                output.write(1); // Method java/lang/Object."<init>":()V
                output.write(OpCodes.RETURN.value);

                var exceptionTableLength = 0;
                output.write(exceptionTableLength & 0xFF00);
                output.write(exceptionTableLength & 0x00FF);

                attributesCount = 2;
                output.write(attributesCount & 0xFF00);
                output.write(attributesCount & 0x00FF);

                //LineNumberTable
                attributeNameIndex = 22;
                output.write(attributeNameIndex & 0xFF00);
                output.write(attributeNameIndex & 0x00FF);

                attributeLength = 6;
                output.write(attributeLength & 0xFF000000);
                output.write(attributeLength & 0x00FF0000);
                output.write(attributeLength & 0x0000FF00);
                output.write(attributeLength & 0x000000FF);

                var lineNumberTableLength = 1;
                output.write(lineNumberTableLength & 0xFF00);
                output.write(lineNumberTableLength & 0x00FF);

                var startPc = 0;
                output.write(startPc & 0xFF00);
                output.write(startPc & 0x00FF);

                var lineNumber = 1;
                output.write(lineNumber & 0xFF00);
                output.write(lineNumber & 0x00FF);

                //LocalVariableTable
                attributeNameIndex = 23;
                output.write(attributeNameIndex & 0xFF00);
                output.write(attributeNameIndex & 0x00FF);

                attributeLength = 0x0C;
                output.write(attributeLength & 0xFF000000);
                output.write(attributeLength & 0x00FF0000);
                output.write(attributeLength & 0x0000FF00);
                output.write(attributeLength & 0x000000FF);

                lineNumberTableLength = 1;
                output.write(lineNumberTableLength & 0xFF00);
                output.write(lineNumberTableLength & 0x00FF);

                startPc = 0;
                output.write(startPc & 0xFF00);
                output.write(startPc & 0x00FF);

                var length = 5;
                output.write(length & 0xFF00);
                output.write(length & 0x00FF);

                nameIndex = 0x18; //this
                output.write(nameIndex & 0xFF00);
                output.write(nameIndex & 0x00FF);

                descriptorIndex = 0x19; //LFoo
                output.write(descriptorIndex & 0xFF00);
                output.write(descriptorIndex & 0x00FF);

                var index = 0;
                output.write(index & 0xFF00);
                output.write(index & 0x00FF);
            }
        }

        main : {
            var flags = AccessFlags.ACC_PUBLIC.value | AccessFlags.ACC_STATIC.value;
            output.write(flags & 0xFF00);
            output.write(flags & 0x00FF);

            var nameIndex = 26; //todo: find in constant pool
            output.write(nameIndex & 0xFF00);
            output.write(nameIndex & 0x00FF);

            var descriptorIndex = 27; //todo: find in constant pool
            output.write(descriptorIndex & 0xFF00);
            output.write(descriptorIndex & 0x00FF);

            var attributesCount = 1;
            output.write(attributesCount & 0xFF00);
            output.write(attributesCount & 0x00FF);

            code : {
                var attributeNameIndex = 21;
                output.write(attributeNameIndex & 0xFF00);
                output.write(attributeNameIndex & 0x00FF);

                var attributeLength = 0x38;
                output.write(attributeLength & 0xFF000000);
                output.write(attributeLength & 0x00FF0000);
                output.write(attributeLength & 0x0000FF00);
                output.write(attributeLength & 0x000000FF);

                var maxStack = 2;
                output.write(maxStack & 0xFF00);
                output.write(maxStack & 0x00FF);

                var maxLocals = 1;
                output.write(maxLocals & 0xFF00);
                output.write(maxLocals & 0x00FF);

                var codeLength = 10;
                output.write(codeLength & 0xFF000000);
                output.write(codeLength & 0x00FF0000);
                output.write(codeLength & 0x0000FF00);
                output.write(codeLength & 0x000000FF);

                output.write(OpCodes.GETSTATIC.value);
                output.write(0); // Field java/lang/System.out:Ljava/io/PrintStream;
                output.write(7); // Field java/lang/System.out:Ljava/io/PrintStream;
                output.write(OpCodes.SIPUSH.value);
                output.write((499 & 0xFF00) >> 8); //TODO: надо везде двигать
                output.write(499 & 0x00FF);
                output.write(OpCodes.INVOKEVIRTUAL.value);
                output.write(0);  // Method java/io/PrintStream.println:(I)V
                output.write(13); // Method java/io/PrintStream.println:(I)V
                output.write(OpCodes.RETURN.value);

                var exceptionTableLength = 0;
                output.write(exceptionTableLength & 0xFF00);
                output.write(exceptionTableLength & 0x00FF);

                attributesCount = 2;
                output.write(attributesCount & 0xFF00);
                output.write(attributesCount & 0x00FF);

                //LineNumberTable
                attributeNameIndex = 22;
                output.write(attributeNameIndex & 0xFF00);
                output.write(attributeNameIndex & 0x00FF);

                attributeLength = 10;
                output.write(attributeLength & 0xFF000000);
                output.write(attributeLength & 0x00FF0000);
                output.write(attributeLength & 0x0000FF00);
                output.write(attributeLength & 0x000000FF);

                var lineNumberTableLength = 2;
                output.write(lineNumberTableLength & 0xFF00);
                output.write(lineNumberTableLength & 0x00FF);

                var startPc = 0;
                output.write(startPc & 0xFF00);
                output.write(startPc & 0x00FF);

                var lineNumber = 3;
                output.write(lineNumber & 0xFF00);
                output.write(lineNumber & 0x00FF);

                startPc = 9;
                output.write(startPc & 0xFF00);
                output.write(startPc & 0x00FF);

                lineNumber = 4;
                output.write(lineNumber & 0xFF00);
                output.write(lineNumber & 0x00FF);

                //LocalVariableTable
                attributeNameIndex = 23;
                output.write(attributeNameIndex & 0xFF00);
                output.write(attributeNameIndex & 0x00FF);

                attributeLength = 0x0C;
                output.write(attributeLength & 0xFF000000);
                output.write(attributeLength & 0x00FF0000);
                output.write(attributeLength & 0x0000FF00);
                output.write(attributeLength & 0x000000FF);

                lineNumberTableLength = 1;
                output.write(lineNumberTableLength & 0xFF00);
                output.write(lineNumberTableLength & 0x00FF);

                startPc = 0;
                output.write(startPc & 0xFF00);
                output.write(startPc & 0x00FF);

                var length = 10;
                output.write(length & 0xFF00);
                output.write(length & 0x00FF);

                nameIndex = 0x1C; //args
                output.write(nameIndex & 0xFF00);
                output.write(nameIndex & 0x00FF);

                descriptorIndex = 0x1D; //[String
                output.write(descriptorIndex & 0xFF00);
                output.write(descriptorIndex & 0x00FF);

                var index = 0;
                output.write(index & 0xFF00);
                output.write(index & 0x00FF);
            }
        }
    }

    private void attributesCount() {
        var attributeCount = 1;
        output.write(attributeCount & 0xFF00);
        output.write(attributeCount & 0x00FF);
    }

    private void attributes() {
        sourceFile : {
            var nameIndex = 30; //todo: find in constant pool
            output.write(nameIndex & 0xFF00);
            output.write(nameIndex & 0x00FF);

            final var length = 2; //always 2
            output.write(length & 0xFF000000);
            output.write(length & 0x00FF0000);
            output.write(length & 0x0000FF00);
            output.write(length & 0x000000FF);

            var sourceFileIndex = 31; //todo: find in constant pool
            output.write(sourceFileIndex & 0xFF00);
            output.write(sourceFileIndex & 0x00FF);
        }
    }
}
