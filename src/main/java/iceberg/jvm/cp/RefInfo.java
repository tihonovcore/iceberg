package iceberg.jvm.cp;

public abstract class RefInfo extends Constant {

    public final int classIndex;
    public final int nameAndTypeIndex;

    public RefInfo(int classIndex, int nameAndTypeIndex) {
        this.classIndex = classIndex;
        this.nameAndTypeIndex = nameAndTypeIndex;
    }
}
