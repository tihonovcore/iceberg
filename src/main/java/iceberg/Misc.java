package iceberg;

public class Misc {

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                if (i % 16 == 0) {
                    hexString.append(System.lineSeparator());
                } else if (i % 8 == 0) {
                    hexString.append(" ");
                }
            }

            String hex = Integer.toHexString(Byte.toUnsignedInt(bytes[i]));
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
            hexString.append(" ");
        }
        return hexString.toString();
    }

    public static class ByteClassLoader extends ClassLoader {

        public ByteClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> define(byte[] bytes) {
            return defineClass("Foo", bytes, 0, bytes.length);
        }
    }
}
