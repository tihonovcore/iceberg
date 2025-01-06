package iceberg.jvm.cp;

import java.nio.charset.StandardCharsets;

public class Utf8 extends Constant {

    final int length;
    final byte[] bytes;

    public Utf8(String s) {
        bytes = s.getBytes(StandardCharsets.UTF_8);
        length = bytes.length;
    }

    @Override
    int tag() {
        return 1;
    }

    @Override
    <T> T accept(ConstantVisitor<T> visitor) {
        return visitor.visitUtf8(this);
    }
}
