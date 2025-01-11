package iceberg.jvm.cp;

public class Noop extends Constant {

    @Override
    int tag() {
        return -1;
    }

    @Override
    <T> T accept(ConstantVisitor<T> visitor) {
        return visitor.visitNoop(this);
    }
}
