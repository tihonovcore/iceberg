package iceberg.common.phases;

import iceberg.SemanticException;
import iceberg.ir.*;

import java.util.ArrayList;
import java.util.List;

public class IrVerificationPhase {

    public void execute(IrFile irFile) {
        var functions = findAllFunctions(irFile);

        functions.forEach(this::atMostOneReturnInBlock);
        functions.forEach(this::noCodeAfterReturn);
        functions.forEach(this::allReturnTypesAreTheSame);
        functions.forEach(this::explicitReturnWhenFunctionReturnTypeSpecified);
    }

    private void atMostOneReturnInBlock(IrFunction irFunction) {
        irFunction.accept(new IrVisitorBase() {
            @Override
            public void visitIrBody(IrBody irBody) {
                super.visitIrBody(irBody);

                var count = irBody.statements.stream()
                    .filter(IrReturn.class::isInstance)
                    .count();

                if (count > 1) {
                    throw new SemanticException("too much return statements in block");
                }
            }
        });
    }

    private void noCodeAfterReturn(IrFunction irFunction) {
        irFunction.accept(new IrVisitorBase() {
            @Override
            public void visitIrBody(IrBody irBody) {
                super.visitIrBody(irBody);

                var optional = irBody.statements.stream()
                    .filter(IrReturn.class::isInstance)
                    .findFirst();
                if (optional.isEmpty()) {
                    return;
                }

                if (optional.get() != irBody.statements.getLast()) {
                    throw new SemanticException("return statement should be at last position in block");
                }
            }
        });
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

    private void explicitReturnWhenFunctionReturnTypeSpecified(IrFunction irFunction) {
        if (irFunction.returnType.equals(IcebergType.unit)) {
            return;
        }

        if (!allBranchesHasReturn(irFunction.irBody)) {
            throw new SemanticException(
                "some branches in '%s' do not have return statement".formatted(irFunction.name)
            );
        }
    }

    private boolean allBranchesHasReturn(IR branch) {
        if (branch instanceof IrReturn) {
            return true;
        }

        if (branch instanceof IrBody irBody) {
            if (irBody.statements.isEmpty()) {
                return false;
            }

            return allBranchesHasReturn(irBody.statements.getLast());
        }

        if (branch instanceof IrIfStatement irIfStatement) {
            if (irIfStatement.elseStatement == null) {
                return allBranchesHasReturn(irIfStatement.thenStatement);
            } else {
                return allBranchesHasReturn(irIfStatement.thenStatement)
                    && allBranchesHasReturn(irIfStatement.elseStatement);
            }
        }

        return false;
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
