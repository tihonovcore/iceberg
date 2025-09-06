package iceberg;

import iceberg.fe.CompilationException;
import iceberg.fe.ParsingUtil;
import iceberg.jvm.phases.validation.EvaluateStackMapAttributePhase;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.CodeGenerator;
import iceberg.jvm.phases.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class CompilationPipeline {

    //TODO: доработать под cli, сделать jar
    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException {
        var dummySource = """
            print 5000; print 100;
            print 123456789;
            \tprint 0; print 9;
            print false or true;
            print "foo";
            """;
        var bytes = compile(dummySource).iterator().next().bytes;

        var path = Path.of("/Users/tihonovcore/IdeaProjects/iceberg/src/main/resources/Iceberg.class");
        Files.write(path, bytes, CREATE, WRITE);

        var classLoader = new Misc.ByteClassLoader();
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

            //compilation process
            new DetectInvalidSyntaxPhase().execute(file);

            var irFile = new BuildIrTreePhase().execute(file);
            new IrVerificationPhase().execute(irFile);

            var compilationUnits = new MoveEachClassToSeparateUnitPhase().execute(irFile);

            for (var unit : compilationUnits) {
                new GenerateDefaultConstructorPhase().execute(unit);
                new GenerateMethodsPhase().execute(unit);
                new GenerateFieldsPhase().execute(unit);
                new CodeAttributeGenerationPhase().execute(unit);
                new EvaluateStackMapAttributePhase().execute(unit);
            }

            //codegen
            CodeGenerator.codegen(compilationUnits);

            return compilationUnits;
        } catch (CompilationException exception) {
            System.err.println(exception.getMessage());
            throw exception;
        }
    }
}
