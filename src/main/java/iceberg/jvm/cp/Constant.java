package iceberg.jvm.cp;

public abstract class Constant {
    abstract int tag();
    abstract <T> T accept(ConstantVisitor<T> visitor);
}