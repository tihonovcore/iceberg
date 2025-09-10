package iceberg.jvm.target;

import iceberg.jvm.cp.ConstantPool;
import iceberg.jvm.cp.Klass;
import iceberg.ir.IrClass;

import java.util.ArrayList;
import java.util.List;

public class CompilationUnit {

    public ConstantPool constantPool = new ConstantPool();

    public Klass thisRef;
    public Klass superRef;

    public List<Object> interfaces = new ArrayList<>();
    public List<Field> fields = new ArrayList<>();
    public List<Method> methods = new ArrayList<>();
    public List<SourceAttribute> attributes = new ArrayList<>();

    public IrClass irClass;

    public byte[] bytes;
}
