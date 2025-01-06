package iceberg.jvm.cp;

public class FieldRef extends RefInfo {

    public FieldRef(int classIndex, int nameAndTypeIndex) {
        super(classIndex, nameAndTypeIndex);
    }

    @Override
    int tag() {
        return 9;
    }

    @Override
    <T> T accept(ConstantVisitor<T> visitor) {
        return visitor.visitFieldRef(this);
    }
}
