package iceberg.jvm.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IrClass implements IR {

    public final String name;
    public final List<IrVariable> fields = new ArrayList<>();
    public final List<IrFunction> methods = new ArrayList<>();
    public final IrFunction defaultConstructor = new IrFunction(this, "<init>", IcebergType.unit);
    //TODO: type

    public Optional<IrFunction> findMethod(String name, List<IcebergType> parametersTypes) {
        return methods.stream()
            .filter(method -> method.name.equals(name))
            .filter(method -> {
                var actualTypes = method.parameters.stream()
                    .map(parameter -> parameter.type)
                    .toList();
                return actualTypes.equals(parametersTypes);
            }).findFirst();
    }

    public IrClass(String name) {
        this.name = name;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrClass(this);
    }
}
