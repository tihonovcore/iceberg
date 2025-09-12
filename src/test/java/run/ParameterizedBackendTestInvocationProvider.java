package run;

import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.provider.Arguments;
import run.compiler.Compiler;
import run.compiler.JvmCompiler;
import run.compiler.LlvmCompiler;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParameterizedBackendTestInvocationProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return context.getTestMethod()
            .map(m -> m.isAnnotationPresent(ParameterizedBackendTest.class))
            .orElse(false);
    }

    @Override
    @SneakyThrows
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        ParameterizedBackendTest annotation = context
            .getRequiredTestMethod()
            .getAnnotation(ParameterizedBackendTest.class);

        var methodName = context.getRequiredTestMethod().getName();
        var methodSource = Arrays.stream(context.getTestClass().orElseThrow().getDeclaredMethods())
            .filter(method -> method.getName().equals(methodName))
            .filter(method -> (method.getModifiers() & Modifier.STATIC) != 0)
            .findFirst().orElseThrow();
        methodSource.setAccessible(true);

        return Arrays.stream(annotation.value())
            .flatMap(target -> {
                try {
                    var arguments = (Stream<Arguments>) methodSource.invoke(null);
                    return arguments.map(args -> invocationContextForBackend(target, args));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
    }

    private TestTemplateInvocationContext invocationContextForBackend(
        BackendTarget target, Arguments arguments
    ) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return target + ", " + Arrays.stream(arguments.get())
                    .map(String::valueOf)
                    .collect(Collectors.joining(" "));
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                var list = new ArrayList<Extension>();
                list.add(new ParameterResolver() {
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

                for (int i = 1; i <= arguments.get().length; i++) {
                    var expectedName = "arg" + i;
                    var argument = arguments.get()[i - 1];

                    list.add(new ParameterResolver() {
                        @Override
                        public boolean supportsParameter(
                            ParameterContext parameterContext,
                            ExtensionContext extensionContext
                        ) {
                            return parameterContext.getParameter().getName().equals(expectedName);
                        }

                        @Override
                        public Object resolveParameter(
                            ParameterContext parameterContext,
                            ExtensionContext extensionContext
                        ) throws ParameterResolutionException {
                            return argument;
                        }
                    });
                }

                return list;
            }
        };
    }
}
