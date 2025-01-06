package iceberg.jvm.cp;

public class MethodRef extends RefInfo {

    public MethodRef(int classIndex, int nameAndTypeIndex) {
        super(classIndex, nameAndTypeIndex);
    }

    @Override
    int tag() {
        return 10;
    }

    @Override
    <T> T accept(ConstantVisitor<T> visitor) {
        return visitor.visitMethodRef(this);
    }
}
