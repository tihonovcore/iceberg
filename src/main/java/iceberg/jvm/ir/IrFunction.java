package iceberg.jvm.ir;

import java.util.ArrayList;
import java.util.List;

public class IrFunction implements IR {

    public final List<IrVariable> parameters = new ArrayList<>();
    public final IrBody irBody = new IrBody();
    public final String name;
    public final IcebergType returnType;

    public IrFunction(String name, IcebergType returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    @Override
    public void accept(IrVisitor visitor) {
        visitor.visitIrFunction(this);
    }
}
