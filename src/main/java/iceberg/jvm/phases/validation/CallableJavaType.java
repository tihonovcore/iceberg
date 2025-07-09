package iceberg.jvm.phases.validation;

import java.util.List;

public class CallableJavaType extends JavaType {

    final List<String> arguments;

    CallableJavaType(List<String> arguments, String returnType) {
        super(returnType);
        this.arguments = arguments;
    }
}
