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

        pool.add(new Utf8("java/lang/Object"));
        OBJECT = new Klass(pool.size());
        pool.add(OBJECT);

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

    public int findLong(long value) {
        for (int i = 0; i < pool.size(); i++) {
            var constant = pool.get(i);
            if (constant instanceof LongInfo info && info.value() == value) {
                return i + 1; //numeration with 1
            }
        }

        return -1;
    }

    public void addLong(long value) {
        if (findLong(value) == -1) {
            pool.add(new LongInfo(value));
            pool.add(new Noop()); // long take 2 indexes from pool
        }
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

    public StringInfo computeString(String value) {
        var utf8Index = indexOf(computeUtf8(value));

        for (Constant constant : pool) {
            if (constant instanceof StringInfo info && info.stringIndex == utf8Index) {
                return info;
            }
        }

        var constant = new StringInfo(utf8Index);
        pool.add(constant);

        return constant;
    }

    public NameAndType computeNameAndType(Utf8 name, Utf8 descriptor) {
        var nameIndex = indexOf(name);
        var descriptorIndex = indexOf(descriptor);

        for (Constant constant : pool) {
            if (constant instanceof NameAndType info
                && info.nameIndex == nameIndex
                && info.descriptorIndex == descriptorIndex
            ) {
                return info;
            }
        }

        var constant = new NameAndType(nameIndex, descriptorIndex);
        pool.add(constant);

        return constant;
    }

    public MethodRef computeMethodRef(Klass klass, NameAndType nameAndType) {
        var classIndex = indexOf(klass);
        var nameAndTypeIndex = indexOf(nameAndType);

        for (Constant constant : pool) {
            if (constant instanceof MethodRef info
                && info.classIndex == classIndex
                && info.nameAndTypeIndex == nameAndTypeIndex
            ) {
                return info;
            }
        }

        var constant = new MethodRef(classIndex, nameAndTypeIndex);
        pool.add(constant);

        return constant;
    }

    public FieldRef computeFieldRef(Klass klass, NameAndType nameAndType) {
        var classIndex = indexOf(klass);
        var nameAndTypeIndex = indexOf(nameAndType);

        for (Constant constant : pool) {
            if (constant instanceof FieldRef info
                && info.classIndex == classIndex
                && info.nameAndTypeIndex == nameAndTypeIndex
            ) {
                return info;
            }
        }

        var constant = new FieldRef(classIndex, nameAndTypeIndex);
        pool.add(constant);

        return constant;
    }

    public Klass computeKlass(Utf8 utf8) {
        var nameIndex = indexOf(utf8);

        for (Constant constant : pool) {
            if (constant instanceof Klass info && info.nameIndex == nameIndex) {
                return info;
            }
        }

        var constant = new Klass(nameIndex);
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
