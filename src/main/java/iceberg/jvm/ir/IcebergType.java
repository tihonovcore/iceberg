package iceberg.jvm.ir;

import iceberg.SemanticException;

public class IcebergType {

    public static final IcebergType i32 = new IcebergType(new IrClass("i32"));
    public static final IcebergType i64 = new IcebergType(new IrClass("i64"));
    public static final IcebergType bool = new IcebergType(new IrClass("bool"));
    public static final IcebergType unit = new IcebergType(new IrClass("unit"));

    public static final IcebergType object = buildJavaLangObject();
    public static final IcebergType string = buildJavaLangString();
    public static final IcebergType iceberg = buildIceberg();

    public static IcebergType valueOf(String type) {
        return switch (type) {
            case "i32" -> i32;
            case "i64" -> i64;
            case "bool" -> bool;
            case "unit" -> unit;
            case "string" -> string;
            default -> throw new SemanticException("unknown type");
        };
    }

    public final IrClass irClass;

    IcebergType(IrClass irClass) {
        this.irClass = irClass;
    }

    private static IcebergType buildJavaLangString() {
        var irClass = new IrClass("java/lang/String");

        var equals = new IrFunction(irClass, "equals", bool);
        equals.parameters.add(new IrVariable(object, null));

        irClass.methods.add(equals);

        return new IcebergType(irClass);
    }

    private static IcebergType buildJavaLangObject() {
        var irClass = new IrClass("java/lang/Object");

        var constructor = new IrFunction(irClass, "<init>", unit);
        irClass.methods.add(constructor);

        return new IcebergType(irClass);
    }

    private static IcebergType buildIceberg() {
        return new IcebergType(new IrClass("Iceberg"));
    }
}
