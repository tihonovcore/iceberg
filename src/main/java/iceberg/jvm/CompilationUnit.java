package iceberg.jvm;

import iceberg.jvm.cp.ConstantPool;
import iceberg.jvm.cp.Klass;
import iceberg.jvm.cp.Utf8;
import iceberg.jvm.ir.IrBody;

import java.util.ArrayList;
import java.util.List;

public class CompilationUnit {

    public ConstantPool constantPool = new ConstantPool();
    public Klass thisRef = constantPool.ICEBERG;
    public Klass superRef = constantPool.OBJECT;
    public List<Object> interfaces = new ArrayList<>();
    public List<Object> fields = new ArrayList<>();
    public List<Method> methods = new ArrayList<>();
    public List<Attribute> attributes;

    public byte[] bytes;

    public static class Method {

        public enum AccessFlags {

            ACC_PUBLIC(0x0001),
            ACC_STATIC(0x0008),
            ;

            AccessFlags(int value) {
                this.value = value;
            }

            public final int value;
        }

        public int flags;
        public Utf8 name;
        public Utf8 descriptor;
        public List<CodeAttribute> attributes = new ArrayList<>();
    }

    public interface Attribute {
    }

    public static class CodeAttribute implements Attribute {

        public Utf8 attributeName;
        public int maxStack;
        public int maxLocals;
        public byte[] code;
        public IrBody body;
        public List<Object> exceptionTable = List.of();
        public List<Attribute> attributes = List.of();
    }

    public static class LineNumberTableAttribute implements Attribute {
    }

    public static class LocalVariableTableAttribute implements Attribute {
    }
}
