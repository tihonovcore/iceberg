package iceberg.jvm.phases;

import iceberg.SemanticException;
import iceberg.jvm.ir.*;

import java.util.ArrayList;
import java.util.List;

public class IrVerificationPhase {

    //TODO: если у функции returnType!=unit - проверить что во всех ветках есть явный return
    //TODO: если несколько бранчей, то во всех есть return

    //TODO: код после ретерна

    public void execute(IrFile irFile) {
        var functions = findAllFunctions(irFile);

        functions.forEach(this::allReturnTypesAreTheSame);
        functions.forEach(this::noCodeAfterReturn);
        functions.forEach(this::explicitReturnWhenFunctionReturnTypeSpecified);
    }

    private void allReturnTypesAreTheSame(IrFunction irFunction) {
        irFunction.irBody.accept(new IrVisitorBase() {
            @Override
            public void visitIrReturn(IrReturn irReturn) {
                var expected = irFunction.returnType;
                var actual = irReturn.expression != null
                    ? irReturn.expression.type
                    : IcebergType.unit;
                if (expected.equals(actual)) {
                    return;
                }

                throw new SemanticException("""
                    bad return in '%s':
                        expected %s
                        but was  %s"""
                    .formatted(irFunction.name, expected.irClass.name, actual.irClass.name)
                );
            }
        });
    }

    private void noCodeAfterReturn(IrFunction irFunction) {

    }

    private void explicitReturnWhenFunctionReturnTypeSpecified(IrFunction irFunction) {

    }

    private List<IrFunction> findAllFunctions(IrFile irFile) {
        var functions = new ArrayList<IrFunction>();

        irFile.accept(new IrVisitorBase() {
            @Override
            public void visitIrFunction(IrFunction irFunction) {
                functions.add(irFunction);
            }
        });

        return functions;
    }
}
