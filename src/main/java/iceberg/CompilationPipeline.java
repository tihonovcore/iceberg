package iceberg;

import iceberg.antlr.IcebergBaseVisitor;
import iceberg.antlr.IcebergParser;
import iceberg.fe.CompilationException;
import iceberg.fe.ParsingUtil;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.CodeGenerator;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class CompilationPipeline {

    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException {
        var dummySource = """
            print 5000; print 100;
            print 123456789;
            \tprint 0; print 9;
            """;
        var bytes = compile(dummySource).iterator().next().bytes;

        var path = Path.of("/Users/tihonovcore/IdeaProjects/iceberg/src/main/resources/Foo.class");
        Files.write(path, bytes, CREATE, WRITE);

        var classLoader = new Misc.ByteClassLoader(Misc.class.getClassLoader());
        var klass = classLoader.define(bytes);

        var main = Arrays.stream(klass.getMethods())
            .filter(method -> "main".equals(method.getName()))
            .findAny().orElseThrow();

        Object[] arguments = new Object[1];
        arguments[0] = new String[0];
        main.invoke(null, arguments);
    }

    public static Collection<CompilationUnit> compile(String source) {
        try {
            var file = ParsingUtil.parse(source);

            var mainUnit = new CompilationUnit();
            var compilationUnits = new ArrayList<>(List.of(mainUnit));

            //compilation process
            fillConstantPool(file, mainUnit);
            //todo: fill units

            //codegen
            CodeGenerator.codegen(compilationUnits, file);

            return compilationUnits;
        } catch (CompilationException exception) {
            System.err.println(exception.getMessage());
        }

        return null; //TODO: do smth
    }

    //TODO: move
    private static void fillConstantPool(IcebergParser.FileContext file, CompilationUnit unit) {
        file.accept(new IcebergBaseVisitor<>() {
            @Override
            public Object visitExpression(IcebergParser.ExpressionContext ctx) {
                var value = Integer.parseInt(ctx.NUMBER().getText());
                if (value < Short.MIN_VALUE || Short.MAX_VALUE < value) {
                    unit.constantPool.addInteger(value);
                }
                return super.visitExpression(ctx);
            }
        });
    }
}
