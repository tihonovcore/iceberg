package iceberg.jvm.cp;

public interface ConstantVisitor<T> {

    T visitUtf8(Utf8 constant);
    T visitKlass(Klass constant);
    T visitNameAndType(NameAndType constant);
    T visitFieldRef(FieldRef constant);
    T visitMethodRef(MethodRef constant);
    T visitIntegerInfo(IntegerInfo constant);
    T visitLongInfo(LongInfo constant);
    T visitNoop(Noop constant);
}