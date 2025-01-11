package iceberg.jvm.cp;

public class StringInfo extends Constant {

    final int stringIndex;

    public StringInfo(int stringIndex) {
        this.stringIndex = stringIndex;
    }

    @Override
    int tag() {
        return 8;
    }

    @Override
    <T> T accept(ConstantVisitor<T> visitor) {
        return visitor.visitStringInfo(this);
    }
}
