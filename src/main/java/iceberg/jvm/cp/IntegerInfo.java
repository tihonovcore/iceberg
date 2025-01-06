package iceberg.jvm.cp;

public class IntegerInfo extends Constant {

    final int bytes;

    public IntegerInfo(int number) {
        this.bytes = number;
    }

    @Override
    int tag() {
        return 3;
    }

    @Override
    <T> T accept(ConstantVisitor<T> visitor) {
        return visitor.visitIntegerInfo(this);
    }
}
