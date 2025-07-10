package iceberg.jvm.ir;

import iceberg.SemanticException;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IcebergType {

    public static final IcebergType i32 = new IcebergType(new IrClass("i32"));
    public static final IcebergType i64 = new IcebergType(new IrClass("i64"));
    public static final IcebergType bool = new IcebergType(new IrClass("bool"));
    public static final IcebergType unit = new IcebergType(new IrClass("unit"));

    public static final IcebergType object = buildJavaLangObject();
    public static final IcebergType string = buildJavaLangString();
    public static final IcebergType printStream = buildJavaIoPrintStream();

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

    @Nullable
    public static IcebergType valueOf(Class<?> klass) {
        if (klass == int.class) {
            return i32;
        } else if (klass == long.class) {
            return i64;
        } else if (klass == boolean.class) {
            return bool;
        } else if (klass == void.class) {
            return unit;
        } else if (klass == String.class) {
            return string;
        } else {
            return null;
        }
    }

    public final IrClass irClass;

    //TODO: в коде есть сравнение типов через ==
    // нужно или переделать на equals или создавать в одном месте
    // ИДЕЯ: разрешить только IrClass'у создавать себе тип
    public IcebergType(IrClass irClass) {
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

    private static IcebergType buildJavaIoPrintStream() {
        var irClass = new IrClass("java/io/PrintStream");

        var possibleParametersTypes = List.of(
            IcebergType.i32,
            IcebergType.i64,
            IcebergType.bool,
            IcebergType.string,
            IcebergType.object
        );
        for (var parametersType : possibleParametersTypes) {
            var parameter = new IrVariable(parametersType, null);

            var println = new IrFunction(irClass, "println", unit);
            println.parameters.add(parameter);

            irClass.methods.add(println);
        }

        return new IcebergType(irClass);
    }
}
