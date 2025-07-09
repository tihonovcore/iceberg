package iceberg.jvm.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IrFunction implements IR {

    public final IrClass irClass;
    public final List<IrVariable> parameters = new ArrayList<>();
    public final IrBody irBody = new IrBody();
    public final String name;
    public final IcebergType returnType;

    public String javaMethodDescriptor() {
        var defaults = Map.of(
            IcebergType.i32, "I",
            IcebergType.i64, "J",
            IcebergType.bool, "Z",
            IcebergType.string, "Ljava/lang/String;",
            IcebergType.object, "Ljava/lang/Object;",
            IcebergType.unit, "V"
        );

        Function<IcebergType, String> mapping = type -> {
            if (defaults.containsKey(type)) {
                return defaults.get(type);
            }

            return "L%s;".formatted(type.irClass.name);
        };

        var params = parameters.stream()
            .map(v -> v.type)
            .map(mapping)
            .collect(Collectors.joining(""));

        return "(" + params + ")" + mapping.apply(returnType);
    }

    public IrFunction(IrClass irClass, String name, IcebergType returnType) {
        this.irClass = irClass;
        this.name = name;
        this.returnType = returnType;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrFunction(this);
    }
}
