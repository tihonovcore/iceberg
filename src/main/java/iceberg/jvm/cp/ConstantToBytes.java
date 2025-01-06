package iceberg.jvm.cp;

import iceberg.jvm.ByteArray;

public class ConstantToBytes implements ConstantVisitor<byte[]> {

    private static final ConstantToBytes INSTANCE = new ConstantToBytes();

    public static byte[] toBytes(Constant constant) {
        return constant.accept(INSTANCE);
    }

    @Override
    public byte[] visitUtf8(Utf8 constant) {
        var result = new ByteArray();
        result.writeU1(constant.tag());
        result.writeU2(constant.length);
        result.writeBytes(constant.bytes);
        return result.bytes();
    }

    @Override
    public byte[] visitKlass(Klass constant) {
        var result = new ByteArray();
        result.writeU1(constant.tag());
        result.writeU2(constant.nameIndex);
        return result.bytes();
    }

    @Override
    public byte[] visitNameAndType(NameAndType constant) {
        var result = new ByteArray();
        result.writeU1(constant.tag());
        result.writeU2(constant.nameIndex);
        result.writeU2(constant.descriptorIndex);
        return result.bytes();
    }

    @Override
    public byte[] visitFieldRef(FieldRef constant) {
        var result = new ByteArray();
        result.writeU1(constant.tag());
        result.writeU2(constant.classIndex);
        result.writeU2(constant.nameAndTypeIndex);
        return result.bytes();
    }

    @Override
    public byte[] visitMethodRef(MethodRef constant) {
        var result = new ByteArray();
        result.writeU1(constant.tag());
        result.writeU2(constant.classIndex);
        result.writeU2(constant.nameAndTypeIndex);
        return result.bytes();
    }

    @Override
    public byte[] visitIntegerInfo(IntegerInfo constant) {
        var result = new ByteArray();
        result.writeU1(constant.tag());
        result.writeU4(constant.bytes);
        return result.bytes();
    }

    @Override
    public byte[] visitLongInfo(LongInfo constant) {
        var result = new ByteArray();
        result.writeU1(constant.tag());
        result.writeU4(constant.highBytes);
        result.writeU4(constant.lowBytes);
        return result.bytes();
    }
}