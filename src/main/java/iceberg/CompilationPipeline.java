package iceberg;

import iceberg.fe.CompilationException;
import iceberg.fe.ParsingUtil;
import iceberg.jvm.phases.validation.CodegenPrepareStackMapAttributePhase;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.CodeGenerator;
import iceberg.jvm.phases.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;

public class CompilationPipeline {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("""
                Невалидное число аргументов
                ice [-cp <path>] source.ib
                ice [-cp <path>] -run source.ib
                ice [-cp <path>] -jar source.ib
                """);
            return;
        }

        enum Mode { compile, run, jar }
        var mode = Mode.valueOf(args[0].substring(1));

        var sourcePath = Path.of(args[1]);
        var source = Files.readString(sourcePath);

        switch (mode) {
            case run -> {
                var classLoader = new Misc.ByteClassLoader();
                var classes = compile(source).stream()
                    .collect(Collectors.toMap(
                        unit -> unit.irClass.name,
                        unit -> classLoader.define(unit.irClass.name, unit.bytes)
                    ));

                var icebergClass = classes.get("Iceberg");
                var main = Arrays.stream(icebergClass.getMethods())
                    .filter(method -> "main".equals(method.getName()))
                    .findAny().orElseThrow();

                Object[] arguments = new Object[1];
                arguments[0] = new String[0];
                main.invoke(null, arguments);
            }
            case compile -> compile(sourcePath, source);
            case jar -> {
                var paths = compile(sourcePath, source);
                var sourceName = sourcePath.getFileName().toString().split("\\.ib")[0];

                var jarName = sourceName + ".jar";
                var jar = Paths.get(
                    sourcePath.toAbsolutePath().getParent().toString(), jarName
                );

                writeJar(jar, paths);
            }
        }
    }

    private static Collection<Path> compile(Path sourcePath, String source) throws IOException {
        var paths = new ArrayList<Path>();

        for (var unit : compile(source)) {
            var classFileName = unit.irClass.name + ".class";
            var path = Paths.get(
                sourcePath.toAbsolutePath().getParent().toString(),
                classFileName
            );
            paths.add(path);

            Files.write(path, unit.bytes, CREATE, TRUNCATE_EXISTING, WRITE);
        }

        return paths;
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
            }

            //codegen
            for (var unit : compilationUnits) {
                new CodegenPrepareMethodsPhase().execute(unit);
                new CodegenPrepareFieldsPhase().execute(unit);
                new CodegenPrepareCodeAttributePhase().execute(unit);
                new CodegenPrepareStackMapAttributePhase().execute(unit);
            }
            CodeGenerator.codegen(compilationUnits);

            return compilationUnits;
        } catch (CompilationException exception) {
            System.err.println(exception.getMessage());
            throw exception;
        }
    }

    private static void writeJar(Path jarPath, Collection<Path> paths) throws IOException {
        var manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "Iceberg");

        try (
            var fos = new FileOutputStream(jarPath.toFile());
            var jos = new JarOutputStream(fos, manifest)
        ) {
            for (Path classFile : paths) {
                var entryName = classFile.getFileName().toString();

                jos.putNextEntry(new JarEntry(entryName));
                Files.copy(classFile, jos);
                jos.closeEntry();
            }
        }
    }
}
