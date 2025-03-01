package iceberg.jvm.target;

import iceberg.jvm.cp.Utf8;
import iceberg.jvm.ir.IrFunction;

import java.util.ArrayList;
import java.util.List;

public class CodeAttribute implements Attribute {

    public Utf8 attributeName;
    public int maxStack;
    public int maxLocals;

    public IrFunction function;
    public byte[] code;

    public List<Object> exceptionTable = List.of();
    public List<StackMapAttribute> attributes = new ArrayList<>();
}