package iceberg.llvm.phases;

import iceberg.ir.*;
import iceberg.llvm.tac.*;

import java.util.Collection;
import java.util.LinkedList;

public class BuildTacPhase {

    private final IrFile irFile;

    public BuildTacPhase(IrFile irFile) {
        this.irFile = irFile;
    }

    public Collection<TacFunction> execute() {
        var functions = new LinkedList<TacFunction>();

        irFile.accept(new IrVisitorBase() {

            private TacFunction currentFunction;
            private TacTyped returned; //TODO: think about IrVisitor<T>

            @Override
            public void visitIrFunction(IrFunction irFunction) {
                functions.add(new TacFunction());
                currentFunction = functions.getLast();

                irFunction.irBody.accept(this);
            }

            @Override
            public void visitIrPrint(IrPrint irPrint) {
                irPrint.argument.accept(this);
                currentFunction.tac.add(new TacPrint(returned));
            }

            @Override
            public void visitIrBinaryExpression(IrBinaryExpression irExpression) {
                irExpression.left.accept(this);
                var left = returned;
                irExpression.right.accept(this);
                var right = returned;

                var target = new TacVariable(synth(), irExpression.type);
                returned = target;

                currentFunction.tac.add(new TacBinaryOperation(target, left, right, irExpression.operator));
            }

            @Override
            public void visitIrUnaryExpression(IrUnaryExpression irExpression) {
                irExpression.value.accept(this);
                var argument = returned;

                var target = new TacVariable(synth(), irExpression.type);
                returned = target;

                currentFunction.tac.add(new TacUnaryOperation(target, argument, irExpression.operator));
            }

            @Override
            public void visitIrCast(IrCast irCast) {
                irCast.irExpression.accept(this);
                var argument = returned;

                var target = new TacVariable(synth(), irCast.type);
                returned = target;

                currentFunction.tac.add(new TacCast(target, argument));
            }

            @Override
            public void visitIrNumber(IrNumber irNumber) {
                returned = new TacNumber(irNumber.value, irNumber.type);
            }

            @Override
            public void visitIrBool(IrBool irBool) {
                returned = new TacNumber(irBool.value ? 1 : 0, IcebergType.bool);
            }

            @Override
            public void visitIrReturn(IrReturn irReturn) {
                if (irReturn.expression != null) {
                    irReturn.expression.accept(this);
                    currentFunction.tac.add(new TacReturn(returned));
                } else {
                    currentFunction.tac.add(new TacReturn());
                }
            }

            private int freeSynthIndex = 0;

            private String synth() {
                return "%synth_" + freeSynthIndex++;
            }
        });

        return functions;
    }
}
