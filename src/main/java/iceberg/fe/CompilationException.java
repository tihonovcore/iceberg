package iceberg.fe;

public class CompilationException extends RuntimeException {

    public CompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
