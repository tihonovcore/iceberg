package iceberg.jvm.ir;

import java.util.ArrayList;
import java.util.List;

public class IrFile implements IR {

    public final List<IrClass> classes = new ArrayList<>();

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrFile(this);
    }
}
