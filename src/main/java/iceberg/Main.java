package iceberg;

import iceberg.fe.ParsingUtil;
import iceberg.jvm.CodeGenerator;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class Main {

    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        var file = ParsingUtil.parse("""
            print 5000; print 100;
            print 123456789;
            \tprint 0; print 9;
            """);

        var bytes = new CodeGenerator().compile(file);
        System.out.println(bytesToHex(bytes));

        var path = Path.of("/Users/tihonovcore/IdeaProjects/iceberg/src/main/resources/Foo.class");
        Files.write(path, bytes, CREATE, WRITE);

        var classLoader = new ByteClassLoader(Main.class.getClassLoader());
        var klass = classLoader.define(bytes);

        var main = Arrays.stream(klass.getMethods())
            .filter(method -> "main".equals(method.getName()))
            .findAny().orElseThrow();

        Object[] arguments = new Object[1];
        arguments[0] = new String[0];
        main.invoke(null, arguments);
    }

    private static String bytesToHex(byte[] bytes) {
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
