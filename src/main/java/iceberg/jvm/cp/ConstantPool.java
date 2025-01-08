package iceberg.jvm.cp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ConstantPool implements Iterable<Constant> {

    public final Klass OBJECT;
    public final Klass ICEBERG;

    private final List<Constant> pool;

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
        pool.add(new Utf8("Iceberg"));
        pool.add(new Utf8("Code"));
        pool.add(new Utf8("LineNumberTable"));
        pool.add(new Utf8("LocalVariableTable"));
        pool.add(new Utf8("this"));
        pool.add(new Utf8("LIceberg;"));
        pool.add(new Utf8("main"));
        pool.add(new Utf8("([Ljava/lang/String;)V"));
        pool.add(new Utf8("args"));
        pool.add(new Utf8("[Ljava/lang/String;"));
        pool.add(new Utf8("SourceFile"));
        pool.add(new Utf8("Iceberg.java"));

        pool.add(new Utf8("java/lang/Object"));
        OBJECT = new Klass(pool.size());
        pool.add(OBJECT);

        //TODO: для нескольких юнитов нужны разные имена
        pool.add(new Utf8("Iceberg"));
        ICEBERG = new Klass(pool.size());
        pool.add(ICEBERG);

    }

    public int indexOf(Constant constant) {
        return pool.indexOf(constant) + 1;
    }

    public int findInteger(int value) {
        for (int i = 0; i < pool.size(); i++) {
            var constant = pool.get(i);
            if (constant instanceof IntegerInfo info && info.bytes == value) {
                return i + 1; //numeration with 1
            }
        }

        return -1;
    }

    public void addInteger(int value) {
        if (findInteger(value) == -1) {
            pool.add(new IntegerInfo(value));
        }
    }

    public void addLong(long value) {
        throw new IllegalStateException("not implemented");
    }

    public Utf8 computeUtf8(String value) {
        for (Constant constant : pool) {
            if (constant instanceof Utf8 info && Arrays.equals(info.bytes, value.getBytes())) {
                return info;
            }
        }

        var constant = new Utf8(value);
        pool.add(constant);

        return constant;
    }

    public int count() {
        return pool.size() + 1;
    }

    @Override
    public Iterator<Constant> iterator() {
        return pool.iterator();
    }
}
