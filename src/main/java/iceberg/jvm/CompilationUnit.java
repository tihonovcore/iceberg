package iceberg.jvm;

import iceberg.jvm.cp.ConstantPool;
import iceberg.jvm.cp.Klass;

import java.util.ArrayList;
import java.util.List;

public class CompilationUnit {

    public ConstantPool constantPool = new ConstantPool();
    public Klass thisRef = constantPool.ICEBERG;
    public Klass superRef = constantPool.OBJECT;
    public List<Object> interfaces = new ArrayList<>();
    public List<Object> fields = new ArrayList<>();
    //List<Method> methods;
    //List<Attribute> attributes;

    public byte[] bytes;
}
