package iceberg.jvm.ir;

import java.util.*;

public class IrClass implements IR {

    public final String name;
    public final Map<String, IrField> fields = new HashMap<>();
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

                //TODO: надо перенести создание IcebergType в одно место,
                // чтобы их можно было сравнивать через ==
                if (actualTypes.size() != parametersTypes.size()) {
                    return false;
                }

                for (int i = 0; i < actualTypes.size(); i++) {
                    var actualType = actualTypes.get(i);
                    var parametersType = parametersTypes.get(i);

                    //inheritance (считаем что в Object можно положить все что угодно)
                    if (actualType.irClass.name.equals("java/lang/Object")) {
                        continue;
                    }

                    if (!actualType.irClass.name.equals(parametersType.irClass.name)) {
                        return false;
                    }
                }

                return true;
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
