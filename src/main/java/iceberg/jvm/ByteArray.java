package iceberg.jvm;

import java.util.Arrays;

public class ByteArray {

    private byte[] buffer = new byte[100];
    private int count = 0;

    public void writeU1(int b) {
        ensureCapacity(count + 1);
        writeByte(b);
    }

    public void writeU2(int s) {
        ensureCapacity(count + 2);
        writeByte(s >> 8);
        writeByte(s);
    }

    public void writeU4(int i) {
        ensureCapacity(count + 4);
        writeByte(i >> 24);
        writeByte(i >> 16);
        writeByte(i >> 8);
        writeByte(i);
    }

    private void writeByte(int b) {
        buffer[count] = (byte) b;
        count++;
    }

    public void writeBytes(byte[] b) {
        ensureCapacity(count + b.length);
        System.arraycopy(b, 0, buffer, count, b.length);
        count += b.length;
    }

    private void ensureCapacity(int minCapacity) {
        int oldCapacity = buffer.length;
        int minGrowth = minCapacity - oldCapacity;
        if (minGrowth > 0) {
            buffer = Arrays.copyOf(buffer, buffer.length * 2);
        }
    }

    public void putU1(int index, int b) {
        buffer[index] = (byte) b;
    }

    public void putU2(int index, int s) {
        putU1(index, s >> 8);
        putU1(index + 1, s);
    }

    public void putU4(int index, int i) {
        putU1(index, i >> 24);
        putU1(index + 1, i >> 16);
        putU1(index + 2, i >> 8);
        putU1(index + 3, i);
    }

    public byte[] bytes() {
        return Arrays.copyOf(buffer, count);
    }

    public int length() {
        return count;
    }
}
