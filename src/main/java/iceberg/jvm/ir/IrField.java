package iceberg.jvm.ir;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class IrField implements IR {

    public final IrClass irClass;
    public final String fieldName;
    public final IcebergType type;
    public @Nullable IrExpression initializer;

    public String javaFieldDescriptor() {
        var defaults = Map.of(
            IcebergType.i32, "I",
            IcebergType.i64, "J",
            IcebergType.bool, "Z",
            IcebergType.string, "Ljava/lang/String;",
            IcebergType.object, "Ljava/lang/Object;",
            IcebergType.unit, "V"
        );

        if (defaults.containsKey(type)) {
            return defaults.get(type);
        }

        return "L%s;".formatted(type.irClass.name);
    }


    public IrField(IrClass irClass, String fieldName, IcebergType type) {
        this.irClass = irClass;
        this.fieldName = fieldName;
        this.type = type;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrField(this);
    }
}
