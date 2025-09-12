package run;

import org.junit.jupiter.api.extension.*;
import run.compiler.Compiler;
import run.compiler.JvmCompiler;
import run.compiler.LlvmCompiler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class BackendTestInvocationProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return context.getTestMethod()
            .map(m -> m.isAnnotationPresent(BackendTest.class))
            .orElse(false);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        BackendTest annotation = context
            .getRequiredTestMethod()
            .getAnnotation(BackendTest.class);

        return Arrays.stream(annotation.value())
            .map(this::invocationContextForBackend);
    }

    private TestTemplateInvocationContext invocationContextForBackend(BackendTarget target) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return "Backend: " + target;
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(
                        ParameterContext parameterContext,
                        ExtensionContext extensionContext
                    ) {
                        return parameterContext.getParameter().getType().equals(Compiler.class);
                    }

                    @Override
                    public Object resolveParameter(
                        ParameterContext parameterContext,
                        ExtensionContext extensionContext
                    ) {
                        return target == BackendTarget.JVM
                            ? new JvmCompiler()
                            : new LlvmCompiler();
                    }
                });
            }
        };
    }
}
