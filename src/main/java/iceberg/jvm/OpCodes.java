package iceberg.jvm;

import java.util.Arrays;

public enum OpCodes {
    ALOAD_0(0x2A),
    BIPUSH(0x10),
    GETSTATIC(0xB2),
    GOTO(0xA7),
    I2L(0x85),
    IADD(0x60),
    ICONST_0(0x03),
    ICONST_1(0x04),
    IDIV(0X6C),
    IF_ICMPEQ(0x9F),
    IF_ICMPNE(0xA0),
    IF_ICMPLT(0xA1),
    IF_ICMPGE(0xA2),
    IF_ICMPGT(0xA3),
    IF_ICMPLE(0xA4),
    IFEQ(0x99),
    IFNE(0x9A),
    IMUL(0x68),
    INEG(0x74),
    INVOKESPECIAL(0xB7),
    INVOKEVIRTUAL(0xB6),
    ISUB(0x64),
    LADD(0x61),
    LCMP(0x94),
    LDIV(0X6D),
    LMUL(0x69),
    LNEG(0x75),
    LDC(0x12),
    LDC_W(0x13),
    LDC_W2(0x14),
    LSUB(0x65),
    RETURN(0xB1),
    SIPUSH(0x11),
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