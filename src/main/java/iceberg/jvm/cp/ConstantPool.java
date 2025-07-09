package iceberg.jvm.cp;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ConstantPool implements Iterable<Constant> {

    private final List<Constant> pool = new ArrayList<>();

    public Constant load(int i) {
        return pool.get(i - 1); //numeration from 1
    }

    public int indexOf(Constant constant) {
        return pool.indexOf(constant) + 1; //numeration from 1
    }

    public Constant computeInt(int value) {
        for (Constant constant : pool) {
            if (constant instanceof IntegerInfo info && info.bytes == value) {
                return constant;
            }
        }

        var constant = new IntegerInfo(value);
        pool.add(constant);

        return constant;
    }

    public Constant computeLong(long value) {
        for (Constant constant : pool) {
            if (constant instanceof LongInfo info && info.value() == value) {
                return constant;
            }
        }

        var constant = new LongInfo(value);
        pool.add(constant);
        pool.add(new Noop()); // long take 2 indexes from pool

        return constant;
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

    @NotNull
    @Override
    public Iterator<Constant> iterator() {
        return pool.iterator();
    }
}
