package iceberg;

public class SemanticException extends RuntimeException {

    public SemanticException() {
        super();
    }

    public SemanticException(String message) {
        super(message);
    }
}
