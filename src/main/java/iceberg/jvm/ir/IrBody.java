package iceberg.jvm.ir;

import java.util.ArrayList;
import java.util.List;

public class IrBody implements IR {

    public List<IR> statements = new ArrayList<>();

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrBody(this);
    }
}
