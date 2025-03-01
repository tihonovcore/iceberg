package iceberg.jvm.target;

import iceberg.jvm.cp.ConstantPool;
import iceberg.jvm.cp.Klass;
import iceberg.jvm.ir.IrClass;
import iceberg.jvm.ir.IrFile;

import java.util.ArrayList;
import java.util.List;

public class CompilationUnit {

    public ConstantPool constantPool = new ConstantPool();
    public Klass thisRef = constantPool.ICEBERG;
    public Klass superRef = constantPool.OBJECT;
    public List<Object> interfaces = new ArrayList<>();
    public List<Object> fields = new ArrayList<>();
    public List<Method> methods = new ArrayList<>();
    public List<SourceAttribute> attributes = new ArrayList<>();

    public IrFile irFile;   //for main-class with main-function
    public IrClass irClass; //for user-defined class

    public byte[] bytes;
}
