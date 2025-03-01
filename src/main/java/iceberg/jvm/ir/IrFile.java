package iceberg.jvm.ir;

import java.util.ArrayList;
import java.util.List;

public class IrFile implements IR {

    public final List<IrFunction> functions = new ArrayList<>();
    public final List<IrClass> classes = new ArrayList<>();

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrFile(this);
    }
}
