package iceberg.jvm.ir;

public interface IR {

    void accept(IrVisitor visitor);
}
