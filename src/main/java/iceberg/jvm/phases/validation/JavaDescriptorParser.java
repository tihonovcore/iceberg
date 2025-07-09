package iceberg.jvm.phases.validation;

import iceberg.jvm.cp.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

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

        if ("(Ljava/lang/Object;)Z".equals(typeDescriptor)) { //String::equals
            return new CallableJavaType(
                List.of("java/lang/Object"), "boolean"
            );
        } else if ("(Z)V".equals(typeDescriptor)) { //System.out::println
            return new CallableJavaType(
                List.of("boolean"), "void"
            );
        } else if ("(I)V".equals(typeDescriptor)) { //System.out::println
            return new CallableJavaType(
                List.of("int"), "void"
            );
        } else if ("(J)V".equals(typeDescriptor)) { //System.out::println
            return new CallableJavaType(
                List.of("long"), "void"
            );
        } else if ("(Ljava/lang/String;)V".equals(typeDescriptor)) { //System.out::println
            return new CallableJavaType(
                List.of("java/lang/String"), "void"
            );
        } else if ("()V".equals(typeDescriptor)) { //custom
            return new CallableJavaType(
                List.of(), "void"
            );
        } else if ("()Ljava/lang/String;".equals(typeDescriptor)) { //custom
            return new CallableJavaType(
                List.of(), "Ljava/lang/String;"
            );
        } else if ("()I".equals(typeDescriptor)) { //custom
            return new CallableJavaType(
                List.of(), "int"
            );
        } else if ("(ILjava/lang/String;)V".equals(typeDescriptor)) { //custom
            return new CallableJavaType(
                List.of("int", "Ljava/lang/String;"), "void"
            );
        } else if ("(I)I".equals(typeDescriptor)) { //custom
            return new CallableJavaType(
                List.of("int"), "int"
            );
        } else if ("(J)J".equals(typeDescriptor)) { //custom
            return new CallableJavaType(
                List.of("long"), "long"
            );
        } else if ("(JI)J".equals(typeDescriptor)) { //custom
            return new CallableJavaType(
                List.of("long", "int"), "long"
            );
        } else if ("(IZ)Z".equals(typeDescriptor)) { //custom
            return new CallableJavaType(
                List.of("int", "boolean"), "boolean"
            );
        } else if ("(ZZZ)Z".equals(typeDescriptor)) { //custom
            return new CallableJavaType(
                List.of("boolean", "boolean", "boolean"), "boolean"
            );
        } else if ("(Ljava/lang/String;)Ljava/lang/String;".equals(typeDescriptor)) { //custom
            return new CallableJavaType(
                List.of("Ljava/lang/String;"), "Ljava/lang/String;"
            );
        } else {
            throw new IllegalStateException("not implemented: " + typeDescriptor);
        }
    }
}
