package iceberg.jvm;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ConstantPool implements Iterable<ConstantPool.Constant> {

    public interface ConstantVisitor<T> {

        T visitUtf8(Utf8 constant);
        T visitKlass(Klass constant);
        T visitNameAndType(NameAndType constant);
        T visitFieldRef(FieldRef constant);
        T visitMethodRef(MethodRef constant);
    }

    public static class ConstantToBytes implements ConstantVisitor<byte[]> {

        private static final ConstantToBytes INSTANCE = new ConstantToBytes();

        public static byte[] toBytes(Constant constant) {
            return constant.accept(INSTANCE);
        }

        @Override
        public byte[] visitUtf8(Utf8 constant) {
            var result = new ByteArrayOutputStream();
            result.write(constant.tag());
            result.write(constant.length & 0xFF00);
            result.write(constant.length & 0x00FF);
            result.writeBytes(constant.bytes);
            return result.toByteArray();
        }

        @Override
        public byte[] visitKlass(Klass constant) {
            var result = new ByteArrayOutputStream();
            result.write(constant.tag());
            result.write(constant.nameIndex & 0xFF00);
            result.write(constant.nameIndex & 0x00FF);
            return result.toByteArray();
        }

        @Override
        public byte[] visitNameAndType(NameAndType constant) {
            var result = new ByteArrayOutputStream();
            result.write(constant.tag());
            result.write(constant.nameIndex & 0xFF00);
            result.write(constant.nameIndex & 0x00FF);
            result.write(constant.descriptorIndex & 0xFF00);
            result.write(constant.descriptorIndex & 0x00FF);
            return result.toByteArray();
        }

        @Override
        public byte[] visitFieldRef(FieldRef constant) {
            var result = new ByteArrayOutputStream();
            result.write(constant.tag());
            result.write(constant.classIndex & 0xFF00);
            result.write(constant.classIndex & 0x00FF);
            result.write(constant.nameAndTypeIndex & 0xFF00);
            result.write(constant.nameAndTypeIndex & 0x00FF);
            return result.toByteArray();
        }

        @Override
        public byte[] visitMethodRef(MethodRef constant) {
            var result = new ByteArrayOutputStream();
            result.write(constant.tag());
            result.write(constant.classIndex & 0xFF00);
            result.write(constant.classIndex & 0x00FF);
            result.write(constant.nameAndTypeIndex & 0xFF00);
            result.write(constant.nameAndTypeIndex & 0x00FF);
            return result.toByteArray();
        }
    }

    public static abstract class Constant {
        abstract int tag();
        abstract <T> T accept(ConstantVisitor<T> visitor);
    }

    public static class Utf8 extends Constant {

        final int length;
        final byte[] bytes;

        public Utf8(String s) {
            bytes = s.getBytes(StandardCharsets.UTF_8);
            length = bytes.length;
        }

        @Override
        int tag() {
            return 1;
        }

        @Override
        <T> T accept(ConstantVisitor<T> visitor) {
            return visitor.visitUtf8(this);
        }
    }

    public static class Klass extends Constant {

        final int nameIndex;

        public Klass(int nameIndex) {
            this.nameIndex = nameIndex;
        }

        @Override
        int tag() {
            return 7;
        }

        @Override
        <T> T accept(ConstantVisitor<T> visitor) {
            return visitor.visitKlass(this);
        }
    }

    public static class NameAndType extends Constant {

        final int nameIndex;
        final int descriptorIndex;

        public NameAndType(int nameIndex, int descriptorIndex) {
            this.nameIndex = nameIndex;
            this.descriptorIndex = descriptorIndex;
        }

        @Override
        int tag() {
            return 12;
        }

        @Override
        <T> T accept(ConstantVisitor<T> visitor) {
            return visitor.visitNameAndType(this);
        }
    }

    public static abstract class RefInfo extends Constant {

        final int classIndex;
        final int nameAndTypeIndex;

        protected RefInfo(int classIndex, int nameAndTypeIndex) {
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }
    }

    public static class FieldRef extends RefInfo {

        protected FieldRef(int classIndex, int nameAndTypeIndex) {
            super(classIndex, nameAndTypeIndex);
        }

        @Override
        int tag() {
            return 9;
        }

        @Override
        <T> T accept(ConstantVisitor<T> visitor) {
            return visitor.visitFieldRef(this);
        }
    }

    public static class MethodRef extends RefInfo {

        protected MethodRef(int classIndex, int nameAndTypeIndex) {
            super(classIndex, nameAndTypeIndex);
        }

        @Override
        int tag() {
            return 10;
        }

        @Override
        <T> T accept(ConstantVisitor<T> visitor) {
            return visitor.visitMethodRef(this);
        }
    }

    private List<Constant> pool;

    public ConstantPool() {
        pool = new ArrayList<>();
        pool.add(new MethodRef(2, 3));
        pool.add(new Klass(4));
        pool.add(new NameAndType(5, 6));
        pool.add(new Utf8("java/lang/Object"));
        pool.add(new Utf8("<init>"));
        pool.add(new Utf8("()V"));
        pool.add(new FieldRef(8, 9));
        pool.add(new Klass(10));
        pool.add(new NameAndType(11, 12));
        pool.add(new Utf8("java/lang/System"));
        pool.add(new Utf8("out"));
        pool.add(new Utf8("Ljava/io/PrintStream;"));
        pool.add(new MethodRef(14, 15));
        pool.add(new Klass(16));
        pool.add(new NameAndType(17, 18));
        pool.add(new Utf8("java/io/PrintStream"));
        pool.add(new Utf8("println"));
        pool.add(new Utf8("(I)V"));
        pool.add(new Klass(20));
        pool.add(new Utf8("Foo"));
        pool.add(new Utf8("Code"));
        pool.add(new Utf8("LineNumberTable"));
        pool.add(new Utf8("LocalVariableTable"));
        pool.add(new Utf8("this"));
        pool.add(new Utf8("LFoo;"));
        pool.add(new Utf8("main"));
        pool.add(new Utf8("([Ljava/lang/String;)V"));
        pool.add(new Utf8("args"));
        pool.add(new Utf8("[Ljava/lang/String;"));
        pool.add(new Utf8("SourceFile"));
        pool.add(new Utf8("Foo.java"));
    }

    public int count() {
        return pool.size() + 1;
    }

    @Override
    public Iterator<Constant> iterator() {
        return pool.iterator();
    }
}
