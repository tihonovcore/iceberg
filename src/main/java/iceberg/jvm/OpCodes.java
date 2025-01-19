package iceberg.jvm;

import java.util.Arrays;

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
    INEG(0x74),
    LNEG(0x75),
    IFEQ(0x99),
    IFNE(0x9A),
    GOTO(0xA7),
    ;

    OpCodes(int value) {
        this.value = value;
    }

    public static OpCodes valueOf(byte value) {
        return Arrays.stream(OpCodes.values())
            .filter(opcode -> opcode.value == (value & 0xFF))
            .findAny().orElseThrow();
    }

    public final int value;
}