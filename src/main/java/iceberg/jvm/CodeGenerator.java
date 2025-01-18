package iceberg.jvm;

import iceberg.jvm.cp.ConstantToBytes;
import iceberg.jvm.target.StackMapAttribute;

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

                output.writeU4(attribute.code.length);
                output.writeBytes(attribute.code);

                output.writeU2(attribute.exceptionTable.size());
                output.writeU2(attribute.attributes.size());

                for (var stackMapAttribute : attribute.attributes) {
                    output.writeU2(compilationUnit.constantPool.indexOf(stackMapAttribute.attributeName));

                    var stackMapAttributeLength = output.lateInitU4();

                    output.writeU2(stackMapAttribute.entries.size());
                    for (var entry : stackMapAttribute.entries) {
                        output.writeU1(entry.frameType); //always full
                        output.writeU2(entry.offsetDelta);

                        output.writeU2(entry.locals.size());
                        //TODO: fill locals

                        output.writeU2(entry.stack.size());
                        for (var frame : entry.stack) {
                            output.writeU1(frame.tag());
                            if (frame instanceof StackMapAttribute.ObjectVariableInfo obj) {
                                output.writeU2(obj.cpoolIndex);
                            }
                        }
                    }

                    stackMapAttributeLength.init();
                }

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
