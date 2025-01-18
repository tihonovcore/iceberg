package iceberg.jvm.cp;

public class NameAndType extends Constant {

    public final int nameIndex;
    public final int descriptorIndex;

    public NameAndType(int nameIndex, int descriptorIndex) {
        this.nameIndex = nameIndex;
        this.descriptorIndex = descriptorIndex;
    }

    @Override
    int tag() {
        return 12;
    }

    @Override
    <T> T accept(ConstantVisitor<T> visitor) {
        return visitor.visitNameAndType(this);
    }
}