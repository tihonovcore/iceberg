package iceberg.jvm;

import iceberg.jvm.cp.ConstantPool;

public class CompilationUnit {

    public ConstantPool constantPool = new ConstantPool();
    //This thisRef;
    //Super superRef;
    //List<Field> fields;
    //List<Method> methods;
    //List<Attribute> attributes;

    public byte[] bytes;
}
