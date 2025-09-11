package iceberg;

import iceberg.jvm.JvmCompiler;
import iceberg.llvm.LlvmCompiler;

import java.nio.file.Files;
import java.nio.file.Path;

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

        enum Mode { compile, run, jar, llvm }
        var mode = Mode.valueOf(args[0].substring(1));

        var sourcePath = Path.of(args[1]);
        var source = Files.readString(sourcePath);

        switch (mode) {
            case run -> JvmCompiler.run(source);
            case compile -> JvmCompiler.compileClasses(sourcePath, source);
            case jar -> JvmCompiler.compileJar(sourcePath, source);
            case llvm -> LlvmCompiler.compile(sourcePath, source);
        }
    }
}
