package iceberg.jvm;

import iceberg.jvm.cp.ConstantPool;
import iceberg.jvm.cp.Klass;
import iceberg.jvm.cp.Utf8;
import iceberg.jvm.ir.IrBody;
import iceberg.jvm.ir.IrFile;
import iceberg.jvm.ir.IrFunction;
import iceberg.jvm.target.SourceAttribute;
import iceberg.jvm.target.StackMapAttribute;

import java.util.ArrayList;
import java.util.List;

//TODO: move all classes to jvm.target
public class CompilationUnit {

    public ConstantPool constantPool = new ConstantPool();
    public Klass thisRef = constantPool.ICEBERG;
    public Klass superRef = constantPool.OBJECT;
    public List<Object> interfaces = new ArrayList<>();
    public List<Object> fields = new ArrayList<>();
    public List<Method> methods = new ArrayList<>();
    public List<SourceAttribute> attributes = new ArrayList<>();

    public IrFile irFile;

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

        public IrFunction function;
        @Deprecated
        public IrBody body;
        public byte[] code;

        public List<Object> exceptionTable = List.of();
        public List<StackMapAttribute> attributes = new ArrayList<>();
    }
}
