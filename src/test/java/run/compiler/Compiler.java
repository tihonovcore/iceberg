package run.compiler;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;

import java.io.File;
import java.io.IOException;

public abstract class Compiler {

    public void execute(String source, String expectedOutput) {
        var workDirectory = Files.newTemporaryFolder();
        try {
            execute(workDirectory, source, expectedOutput);
        } finally {
            try {
                FileUtils.deleteDirectory(workDirectory);
            } catch (IOException e) {
                System.out.println("Failed to delete " + workDirectory);
            }
        }
    }

    protected abstract void execute(
        File workDirectory, String source, String expectedOutput
    );
}
