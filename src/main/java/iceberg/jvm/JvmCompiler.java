package iceberg.jvm;

import iceberg.Misc;
import iceberg.common.phases.BuildIrTreePhase;
import iceberg.common.phases.DetectInvalidSyntaxPhase;
import iceberg.common.phases.IrVerificationPhase;
import iceberg.common.phases.ParseSourcePhase;
import iceberg.jvm.phases.*;
import iceberg.jvm.phases.validation.CodegenPrepareStackMapAttributePhase;
import iceberg.jvm.target.CompilationUnit;
import lombok.SneakyThrows;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;

public class JvmCompiler {

    public static Collection<CompilationUnit> compile(String source) {
        var astFile = new ParseSourcePhase().execute(source);
        new DetectInvalidSyntaxPhase().execute(astFile);

        var irFile = new BuildIrTreePhase().execute(astFile);
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
    }

    @SneakyThrows
    public static void run(String source) {
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

    @SneakyThrows
    public static Collection<Path> compileClasses(Path sourcePath, String source) {
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

    public static void compileJar(Path sourcePath, String source) {
        var paths = compileClasses(sourcePath, source);
        var sourceName = sourcePath.getFileName().toString().split("\\.ib")[0];

        var jarName = sourceName + ".jar";
        var jar = Paths.get(
            sourcePath.toAbsolutePath().getParent().toString(), jarName
        );

        writeJar(jar, paths);
    }

    @SneakyThrows
    private static void writeJar(Path jarPath, Collection<Path> paths) {
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
