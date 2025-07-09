package iceberg.jvm;

import java.util.Arrays;

public enum OpCodes {
    ACONST_NULL(0x01),
    ALOAD_0(0x2A),
    ALOAD(0x19),
    ARETURN(0xB0),
    ASTORE(0x3A),
    BIPUSH(0x10),
    DUP(0x59),
    GETFIELD(0xB4),
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
    ILOAD(0x15),
    IMUL(0x68),
    INEG(0x74),
    INVOKESPECIAL(0xB7),
    INVOKESTATIC(0xB8),
    INVOKEVIRTUAL(0xB6),
    IRETURN(0xAC),
    ISTORE(0x36),
    ISUB(0x64),
    LADD(0x61),
    LCMP(0x94),
    LCONST_0(0x09),
    LDIV(0X6D),
    LLOAD(0x16),
    LMUL(0x69),
    LNEG(0x75),
    LDC(0x12),
    LDC_W(0x13),
    LDC_W2(0x14),
    LRETURN(0XAD),
    LSTORE(0x37),
    LSUB(0x65),
    NEW(0xBB),
    PUTFIELD(0xB5),
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