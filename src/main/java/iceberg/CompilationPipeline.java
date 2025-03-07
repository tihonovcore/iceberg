package iceberg;

import iceberg.fe.CompilationException;
import iceberg.fe.ParsingUtil;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.CodeGenerator;
import iceberg.jvm.phases.*;
import iceberg.jvm.target.SourceAttribute;

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

            var mainUnit = new CompilationUnit();
            mainUnit.attributes.add(new SourceAttribute(
                mainUnit.constantPool.computeUtf8("SourceFile"),
                mainUnit.constantPool.computeUtf8("Iceberg.ib")
            ));

            var compilationUnits = new ArrayList<>(List.of(mainUnit));

            //compilation process
            new DetectInvalidSyntaxPhase().execute(file, mainUnit);

            new BuildIrTreePhase().execute(file, mainUnit);
            new MoveEachClassToSeparateUnitPhase().execute(mainUnit, compilationUnits);

            for (var unit : compilationUnits) {
                //TODO: GenerateFieldsPhase
                new GenerateDefaultConstructor().execute(file, unit);
                new GenerateMethodsPhase().execute(file, unit);
                new ByteCodeGenerationPhase().execute(file, unit);
                new EvaluateStackMapAttributePhase().execute(file, unit);
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
