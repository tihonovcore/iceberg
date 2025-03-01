package iceberg.jvm.ir;

import java.util.ArrayList;
import java.util.List;

public class IrClass implements IR {

    public final String name;
    public final List<IrVariable> fields = new ArrayList<>();
    public final List<IrFunction> methods = new ArrayList<>();
    //TODO: type

    public IrClass(String name) {
        this.name = name;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrClass(this);
    }
}
