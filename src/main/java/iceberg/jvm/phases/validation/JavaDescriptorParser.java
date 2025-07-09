package iceberg.jvm.phases.validation;

import iceberg.jvm.cp.*;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

@RequiredArgsConstructor
public class JavaDescriptorParser {

    private final ConstantPool constantPool;

    public JavaType parse(Constant constant) {
        return switch (constant) {
            case IntegerInfo ignored -> new JavaType("int");
            case LongInfo ignored -> new JavaType("long");
            case StringInfo ignored -> new JavaType("java/lang/String");
            case FieldRef ref -> parseFieldRef(ref);
            case MethodRef ref -> parseMethodRef(ref);
            default -> throw new IllegalStateException("Unexpected value: " + constant);
        };
    }

    private JavaType parseFieldRef(FieldRef ref) {
        var nameAndType = (NameAndType) constantPool.load(ref.nameAndTypeIndex);
        var utf8 = (Utf8) constantPool.load(nameAndType.descriptorIndex);
        var typeDescriptor = new String(utf8.bytes);

        //TODO: decode descriptor - for int it will be I (not int)
        return new JavaType(typeDescriptor.substring(1, typeDescriptor.length() - 1));
    }

    private JavaType parseMethodRef(MethodRef methodRef) {
        var nameAndType = (NameAndType) constantPool.load(methodRef.nameAndTypeIndex);
        var utf8 = (Utf8) constantPool.load(nameAndType.descriptorIndex);
        var typeDescriptor = new String(utf8.bytes);

        var arguments = new ArrayList<String>();

        int i = 1;
        while (i < typeDescriptor.length()) {
            switch (typeDescriptor.charAt(i)) {
                case 'Z' -> arguments.add("boolean");
                case 'V' -> arguments.add("void");
                case 'I' -> arguments.add("int");
                case 'J' -> arguments.add("long");
                case 'L' -> {
                    var end = typeDescriptor.indexOf(';', i);
                    arguments.add(typeDescriptor.substring(i + 1, end));

                    i = end;
                }
                case ')' -> { /* do nothing */ }
            }
            i++;
        }

        return new CallableJavaType(
            arguments.subList(0, arguments.size() - 1), arguments.getLast()
        );
    }
}
