package iceberg.jvm.cp;

public class LongInfo extends Constant {

    final int highBytes;
    final int lowBytes;

    public LongInfo(long number) {
        this.highBytes = (int) (number >> 32);
        this.lowBytes = (int) number;
    }

    public long value() {
        return ((long) highBytes << 32) | (lowBytes & 0xFFFFFFFFL);
    }

    @Override
    int tag() {
        return 5;
    }

    @Override
    <T> T accept(ConstantVisitor<T> visitor) {
        return visitor.visitLongInfo(this);
    }
}
