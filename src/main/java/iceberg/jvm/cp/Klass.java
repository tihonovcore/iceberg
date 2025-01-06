package iceberg.jvm.cp;

public class Klass extends Constant {

    final int nameIndex;

    public Klass(int nameIndex) {
        this.nameIndex = nameIndex;
    }

    @Override
    int tag() {
        return 7;
    }

    @Override
    <T> T accept(ConstantVisitor<T> visitor) {
        return visitor.visitKlass(this);
    }
}
