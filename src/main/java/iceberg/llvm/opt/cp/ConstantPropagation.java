package iceberg.llvm.opt.cp;

import iceberg.llvm.BasicBlock;
import iceberg.llvm.FunctionCfg;
import iceberg.llvm.tac.*;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class ConstantPropagation {

    private final FunctionCfg functionCfg;

    public ConstantPropagation(FunctionCfg functionCfg) {
        this.functionCfg = functionCfg;
    }

    public void execute() {
        VariablesVector undefVector = findAllVars();

        var outputs = new HashMap<String, VariablesVector>();
        functionCfg.bbs.values().stream()
            .map(bb -> bb.label)
            .forEach(label -> outputs.put(label, undefVector.copy()));

        boolean changed = true;
        while (changed) {
            changed = false;

            for (var bb : functionCfg.bbs.values()) {
                var input = undefVector.copy();
                for (var prev : bb.prev) {
                    var output = outputs.get(prev.label);
                    input = input.join(output);
                }

                var oldOutput = outputs.get(bb.label);
                var newOutput = interpret(input, bb);
                if (!oldOutput.equals(newOutput)) {
                    outputs.put(bb.label, newOutput);
                    changed = true;
                }
            }
        }

        //propagate constants
        for (var bb : functionCfg.bbs.values()) {
            var output = outputs.get(bb.label);
            output.values.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Value.Const)
                .forEach(entry -> replace(bb, entry.getKey(), (Value.Const) entry.getValue()));
        }
    }

    //TODO: replace Store
    //TODO: достаточно ли менять tac в BBS или нужно и в FunctionCfg?
    private void replace(BasicBlock basicBlock, String varName, Value.Const value) {
        var constant = value.value;
        for (int i = 0; i < basicBlock.tac.size(); i++) {
            var tac = basicBlock.tac.get(i);
            if (tac instanceof TacBinaryOperation binOp) {
                if (binOp.left instanceof TacVariable var && var.name.equals(varName)) {
                    tac = new TacBinaryOperation(
                        binOp.target, constant, binOp.right, binOp.operator
                    );
                }
                if (binOp.right instanceof TacVariable var && var.name.equals(varName)) {
                    tac = new TacBinaryOperation(
                        binOp.target, binOp.left, constant, binOp.operator
                    );
                }
            } else if (tac instanceof TacPrint print) {
                if (print.argument instanceof TacVariable var && var.name.equals(varName)) {
                    tac = new TacPrint(constant);
                }
            }

            basicBlock.tac.set(i, tac);
        }
    }

    private VariablesVector interpret(VariablesVector input, BasicBlock basicBlock) {
        System.out.println("interpret " + basicBlock.label);
        var current = new AtomicReference<>(input);
        basicBlock.tac.forEach(tac -> tac.accept(new TacVisitorBase() {

            @Override
            public void visitTacBinaryOperation(TacBinaryOperation tacBinaryOperation) {
                Value left;
                if (tacBinaryOperation.left instanceof TacNumber num) {
                    left = new Value.Const(num);
                } else if (tacBinaryOperation.left instanceof TacVariable var) {
                    left = current.get().values.get(var.name);
                } else {
                    throw new IllegalStateException("impossible");
                }

                Value right;
                if (tacBinaryOperation.right instanceof TacNumber num) {
                    right = new Value.Const(num);
                } else if (tacBinaryOperation.right instanceof TacVariable var) {
                    right = current.get().values.get(var.name);
                } else {
                    throw new IllegalStateException("impossible");
                }

                Value result;
                if (left instanceof Value.Const cl && right instanceof Value.Const cr) {
                    var number = switch (tacBinaryOperation.operator) {
                        case MULT -> cl.value.value * cr.value.value;
                        case DIV -> cl.value.value / cr.value.value;
                        case PLUS -> cl.value.value + cr.value.value;
                        case SUB -> cl.value.value - cr.value.value;
                        case LE -> cl.value.value <= cr.value.value ? 1 : 0;
                        case LT -> cl.value.value < cr.value.value ? 1 : 0;
                        case EQ -> cl.value.value == cr.value.value ? 1 : 0;
                        case AND -> cl.value.value + cr.value.value == 2 ? 1 : 0;
                        case OR -> cl.value.value + cr.value.value != 0 ? 1 : 0;
                    };
                    result = new Value.Const(new TacNumber(number, tacBinaryOperation.target.type));
                } else {
                    result = Value.OVERDEF;
                }

                var in = current.get().values.get(tacBinaryOperation.target.name);

                var next = current.get().copy();
                next.values.put(tacBinaryOperation.target.name, in.join(result));

                current.set(next);
            }

            @Override
            public void visitTacCast(TacCast tacCast) {
                //TODO
            }

            @Override
            public void visitTacUnaryOperation(TacUnaryOperation tacUnaryOperation) {
                //TODO
            }

            @Override
            public void visitTacVarLoad(TacVarLoad tacVarLoad) {
                var in = current.get().values.get(tacVarLoad.target.name);
                var argument = current.get().values.get(tacVarLoad.memory.name);

                var next = current.get().copy();
                next.values.put(tacVarLoad.target.name, in.join(argument));

                current.set(next);
            }

            @Override
            public void visitTacVarStore(TacVarStore tacVarStore) {
                var in = current.get().values.get(tacVarStore.target.name);
                if (tacVarStore.argument instanceof TacNumber num) {
                    var out = new Value.Const(num);

                    var next = current.get().copy();
                    next.values.put(tacVarStore.target.name, in.join(out));

                    current.set(next);
                }

                if (tacVarStore.argument instanceof TacVariable var) {
                    var out = current.get().values.get(var.name);

                    var next = current.get().copy(); //TODO: можно не делать?
                    next.values.put(tacVarStore.target.name, in.join(out));

                    current.set(next);
                }
            }
        }));

        System.out.println("## " + basicBlock.label);
        System.out.println(input.values);
        System.out.println("####");
        System.out.println(current.get().values);

        return current.get();
    }

    private VariablesVector findAllVars() {
        var codes = functionCfg.bbs.values().stream()
            .flatMap(basicBlock -> basicBlock.tac.stream())
            .toList();

        var variables = new HashMap<String, Value>();
        for (var tac : codes) {
            tac.accept(new TacVisitorBase() {
                @Override
                public void visitTacBinaryOperation(TacBinaryOperation tacBinaryOperation) {
                    variables.put(tacBinaryOperation.target.name, Value.UNDEF);
                }

                @Override
                public void visitTacCast(TacCast tacCast) {
                    variables.put(tacCast.target.name, Value.UNDEF);
                }

                @Override
                public void visitTacUnaryOperation(TacUnaryOperation tacUnaryOperation) {
                    variables.put(tacUnaryOperation.target.name, Value.UNDEF);
                }

                @Override
                public void visitTacVarAllocate(TacVarAllocate tacVarAllocate) {
                    variables.put(tacVarAllocate.target.name, Value.UNDEF);
                }

                @Override
                public void visitTacVarLoad(TacVarLoad tacVarLoad) {
                    variables.put(tacVarLoad.target.name, Value.UNDEF);
                }
            });
        }

        return new VariablesVector(variables);
    }
}
