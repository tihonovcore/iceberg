package iceberg.ir;

public interface IR {

    void accept(IrVisitor visitor);
}
