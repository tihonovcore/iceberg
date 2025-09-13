package iceberg.llvm.phases;

import iceberg.ir.IcebergType;
import iceberg.llvm.BasicBlock;
import iceberg.llvm.FunctionCfg;
import iceberg.llvm.tac.*;

import java.util.Collection;

public class CodeGenerationPhase {

    private final Collection<FunctionCfg> allCfg;

    public CodeGenerationPhase(Collection<FunctionCfg> allCfg) {
        this.allCfg = allCfg;
    }

    public String execute() {
        var output = new StringBuilder();
        //TODO: вычислять target на основе System.getProperty("os.arch") и тому подобному
        output.append("target triple = \"arm64-apple-macosx15.0.0\"");
        output.append(System.lineSeparator());
        output.append(System.lineSeparator());

        allCfg.forEach(functionCfg -> {
            output.append("define i32 @");
            output.append(functionCfg.irFunction.name);
            output.append("() {");
            output.append(System.lineSeparator());

            functionCfg.bbs.values()
                .forEach(basicBlock -> dumpBasicBlock(output, basicBlock));

            output.append("}");
            output.append(System.lineSeparator());
            output.append(System.lineSeparator());
        });

        output.append("""
            ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
            ; io utils begin
            @.fmt32 = private constant [4 x i8] c"%d\\0A\\00"
            @.fmt64 = private unnamed_addr constant [5 x i8] c"%ld\\0A\\00"
            @.true_str  = private constant [6 x i8] c"true\\0A\\00"
            @.false_str = private constant [7 x i8] c"false\\0A\\00"
            
            declare i32 @printf(i8*, ...)
            
            define void @print_i1(i1 %val) {
            entry:
              %sel = select i1 %val,\s
                            i8* getelementptr inbounds ([6 x i8], [6 x i8]* @.true_str, i64 0, i64 0),\s
                            i8* getelementptr inbounds ([7 x i8], [7 x i8]* @.false_str, i64 0, i64 0)
            
              call i32 (i8*, ...) @printf(i8* %sel)
              ret void
            }
            
            define void @print_i32(i32 %x) {
            entry:
              %fmt_ptr = getelementptr [4 x i8], [4 x i8]* @.fmt32, i32 0, i32 0
              call i32 (i8*, ...) @printf(i8* %fmt_ptr, i32 %x)
              ret void
            }
            
            define void @print_i64(i64 %x) {
            entry:
              %fmt_ptr = getelementptr [4 x i8], [4 x i8]* @.fmt64, i32 0, i32 0
              call i32 (i8*, ...) @printf(i8* %fmt_ptr, i64 %x)
              ret void
            }
            ; io utils end
            ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
            """);

        return output.toString();
    }

    private void dumpBasicBlock(StringBuilder output, BasicBlock basicBlock) {
        output.append(basicBlock.label);
        output.append(":");
        output.append(System.lineSeparator());

        basicBlock.tac.forEach(tac -> tac.accept(new TacVisitor() {

            private final String indent = "    ";

            @Override
            public void visitTacBinaryOperation(TacBinaryOperation tacBinaryOperation) {
                output.append(indent);
                tacBinaryOperation.target.accept(this);
                output.append(" = ");
                output.append(switch (tacBinaryOperation.operator) {
                    case PLUS -> "add";
                    case SUB -> "sub";
                    case MULT -> "mul";
                    case DIV -> "sdiv";
                    case LE -> "icmp sle";
                    case LT -> "icmp slt";
                    case EQ -> "icmp eq";
                    case AND -> "and";
                    case OR -> "or";
                });

                //TODO: assert that left.type.equals(right.type)
                output.append(" ");
                output.append(mapType(tacBinaryOperation.left.type));
                output.append(" ");

                tacBinaryOperation.left.accept(this);
                output.append(", ");
                tacBinaryOperation.right.accept(this);
                output.append(System.lineSeparator());
            }

            @Override
            public void visitTacCast(TacCast tacCast) {
                output.append(indent);
                tacCast.target.accept(this);
                output.append(" = ");
                output.append("sext i32 ");
                tacCast.argument.accept(this);
                output.append(" to i64");
                output.append(System.lineSeparator());
            }

            @Override
            public void visitTacJump(TacJump tacJump) {
                output.append(indent);
                output.append("br label %");
                output.append(tacJump.gotoLabel);
                output.append(System.lineSeparator());
            }

            @Override
            public void visitTacJumpConditional(TacJumpConditional tacJumpConditional) {
                output.append(indent);
                output.append("br i1 ");
                tacJumpConditional.condition.accept(this);
                output.append(", label %");
                output.append(tacJumpConditional.thenLabel);
                output.append(", label %");
                output.append(tacJumpConditional.elseLabel);
                output.append(System.lineSeparator());
            }

            @Override
            public void visitTacNumber(TacNumber tacNumber) {
                output.append(tacNumber.value);
            }

            @Override
            public void visitTacPrint(TacPrint tacPrint) {
                output.append(indent);

                output.append("call void @print_");
                output.append(mapType(tacPrint.argument.type));
                output.append("(");
                output.append(mapType(tacPrint.argument.type));
                output.append(" ");

                tacPrint.argument.accept(this);
                output.append(")");
                output.append(System.lineSeparator());
            }

            @Override
            public void visitTacReturn(TacReturn tacReturn) {
                output.append(indent);
                output.append("ret i32 0"); //TODO: get type from argument
                output.append(System.lineSeparator());
            }

            @Override
            public void visitTacUnaryOperation(TacUnaryOperation tacUnaryOperation) {
                output.append(indent);
                tacUnaryOperation.target.accept(this);
                output.append(" = ");

                switch (tacUnaryOperation.operator) {
                    case NOT -> {
                        output.append("xor i1 ");
                        tacUnaryOperation.argument.accept(this);
                        output.append(", true");
                    }
                    case MINUS -> {
                        output.append("sub ");
                        output.append(
                            tacUnaryOperation.argument.type.equals(IcebergType.i32)
                                ? "i32 "
                                : "i64 "
                        );
                        output.append("0, ");
                        tacUnaryOperation.argument.accept(this);
                    }
                }

                output.append(System.lineSeparator());
            }

            @Override
            public void visitTacVarAllocate(TacVarAllocate tacVarAllocate) {
                output.append(indent);
                output.append(tacVarAllocate.target);
                output.append(" = alloca ");
                output.append(mapType(tacVarAllocate.target.type));
                output.append(System.lineSeparator());
            }

            @Override
            public void visitTacVarLoad(TacVarLoad tacVarLoad) {
                output.append(indent);
                output.append(tacVarLoad.target);
                output.append(" = load ");
                output.append(mapType(tacVarLoad.target.type));
                output.append(", ");
                output.append(mapType(tacVarLoad.memory.type));
                output.append("* ");
                output.append(tacVarLoad.memory);
                output.append(System.lineSeparator());
            }

            @Override
            public void visitTacVarStore(TacVarStore tacVarStore) {
                output.append(indent);
                output.append("store ");
                output.append(mapType(tacVarStore.argument.type));
                output.append(" ");
                output.append(tacVarStore.argument);
                output.append(", ");
                output.append(mapType(tacVarStore.target.type));
                output.append("* ");
                output.append(tacVarStore.target);
                output.append(System.lineSeparator());
            }

            @Override
            public void visitTacVariable(TacVariable tacVariable) {
                output.append(tacVariable.name);
            }
        }));
    }

    //TODO: support all types
    private String mapType(IcebergType icebergType) {
        if (icebergType.equals(IcebergType.bool)) {
            return "i1";
        } else if (icebergType.equals(IcebergType.i32)) {
            return "i32";
        } else if (icebergType.equals(IcebergType.i64)) {
            return "i64";
        } else {
            throw new IllegalStateException("not yet implemented");
        }
    }
}
