package iceberg.jvm;

public enum OpCodes {
    ICONST_0(0x03),
    ICONST_1(0x04),
    ALOAD_0(0x2A),
    RETURN(0xB1),
    GETSTATIC(0xB2),
    INVOKEVIRTUAL(0xB6),
    INVOKESPECIAL(0xB7),
    BIPUSH(0x10),
    SIPUSH(0x11),
    LDC(0x12),
    LDC_W(0x13),
    LDC_W2(0x14),
    IAND(0x7E),
    IOR(0x80),
    ;

    OpCodes(int value) {
        this.value = value;
    }

    public final int value;
}