package iceberg.llvm.phases;

import iceberg.ir.*;
import iceberg.llvm.FunctionTac;
import iceberg.llvm.tac.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class BuildTacPhase {

    private final IrFile irFile;

    public BuildTacPhase(IrFile irFile) {
        this.irFile = irFile;
    }

    public Collection<FunctionTac> execute() {
        var functions = new LinkedList<FunctionTac>();

        irFile.accept(new IrVisitorBase() {

            private FunctionTac currentFunction;
            private TacTyped returned; //TODO: think about IrVisitor<T>

            @Override
            public void visitIrFunction(IrFunction irFunction) {
                functions.add(new FunctionTac(irFunction));
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
            public void visitIrIfStatement(IrIfStatement irIfStatement) {
                irIfStatement.condition.accept(this);
                var condition = returned;

                var fromCondition = new TacJumpConditional(condition);
                currentFunction.tac.add(fromCondition);

                fromCondition.thenOffset = currentFunction.tac.size();
                irIfStatement.thenStatement.accept(this);

                var fromThenBody = new TacJump();
                currentFunction.tac.add(fromThenBody);

                if (irIfStatement.elseStatement == null) {
                    fromCondition.elseOffset = currentFunction.tac.size();
                    fromThenBody.gotoOffset = currentFunction.tac.size();
                    return;
                }

                fromCondition.elseOffset = currentFunction.tac.size();
                irIfStatement.elseStatement.accept(this);

                var fromElseBody = new TacJump();
                currentFunction.tac.add(fromElseBody);

                fromThenBody.gotoOffset = currentFunction.tac.size();
                fromElseBody.gotoOffset = currentFunction.tac.size();
            }

            @Override
            public void visitIrLoop(IrLoop irLoop) {
                //NOTE: if where are TACs before condition,
                //they should unconditionally jump to condition bb
                if (!currentFunction.tac.isEmpty()) {
                    var jumpToConditionBlock = new TacJump();
                    currentFunction.tac.add(jumpToConditionBlock);
                    jumpToConditionBlock.gotoOffset = currentFunction.tac.size();
                }

                var conditionOffset = currentFunction.tac.size();

                irLoop.condition.accept(this);
                var condition = returned;

                var fromCondition = new TacJumpConditional(condition);
                currentFunction.tac.add(fromCondition);

                fromCondition.thenOffset = currentFunction.tac.size();
                irLoop.body.accept(this);

                var fromBody = new TacJump();
                currentFunction.tac.add(fromBody);
                fromBody.gotoOffset = conditionOffset;

                fromCondition.elseOffset = currentFunction.tac.size();
            }

            @Override
            public void visitIrAssignVariable(IrAssignVariable irAssignVariable) {
                var target = new TacVariable(
                    allocated.get(irAssignVariable.definition),
                    irAssignVariable.definition.type
                );

                irAssignVariable.expression.accept(this);
                var argument = returned;

                var store = new TacVarStore(target, argument);
                currentFunction.tac.add(store);

                returned = target;
            }

            @Override
            public void visitIrReadVariable(IrReadVariable irReadVariable) {
                var target = new TacVariable(synth(), irReadVariable.type);
                var memory = new TacVariable(
                    allocated.get(irReadVariable.definition),
                    irReadVariable.definition.type
                );

                var load = new TacVarLoad(target, memory);
                currentFunction.tac.add(load);

                returned = target;
            }

            private final Map<IrVariable, String> allocated = new HashMap<>();

            @Override
            public void visitIrVariable(IrVariable irVariable) {
                var target = new TacVariable(synth(), irVariable.type);
                allocated.put(irVariable, target.name);

                var alloc = new TacVarAllocate(target);
                currentFunction.tac.add(alloc);

                if (irVariable.initializer != null) {
                    irVariable.initializer.accept(this);
                    var argument = returned;

                    var store = new TacVarStore(target, argument);
                    currentFunction.tac.add(store);
                }

                returned = target;
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

        //debug print
        functions.forEach(function -> {
            for (int i = 0; i < function.tac.size(); i++) {
                System.out.println(i + " " + function.tac.get(i));
            }
        });
        System.out.println("======================================");

        return functions;
    }
}
