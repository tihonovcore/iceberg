package iceberg.llvm.phases;

import iceberg.ir.IcebergType;
import iceberg.llvm.BasicBlock;
import iceberg.llvm.tac.*;

import java.util.Collection;

public class CodeGenerationPhase {

    private final Collection<BasicBlock> allCfg;

    public CodeGenerationPhase(Collection<BasicBlock> allCfg) {
        this.allCfg = allCfg;
    }

    public String execute() {
        var output = new StringBuilder();
        //TODO: вычислять target на основе System.getProperty("os.arch") и тому подобному
        output.append("target triple = \"arm64-apple-macosx15.0.0\"");
        output.append(System.lineSeparator());
        output.append(System.lineSeparator());
        output.append("""
            ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
            ; io utils begin
            @.fmt = private constant [4 x i8] c"%d\\0A\\00"
            @.fmt64 = private unnamed_addr constant [5 x i8] c"%ld\\0A\\00", align 1
            
            declare i32 @printf(i8*, ...)
            
            define void @print_i32(i32 %x) {
            entry:
              %fmt_ptr = getelementptr [4 x i8], [4 x i8]* @.fmt, i32 0, i32 0
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
        output.append(System.lineSeparator());
        output.append(System.lineSeparator());

        allCfg.forEach(basicBlock -> {
            //TODO: IrFunction.name needed
            output.append("define i32 @main() {");
            output.append(System.lineSeparator());

            //TODO: do recursively, use basicBlock.next
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
                        //TODO: check these
                        case LE -> "LE";
                        case LT -> "LT";
                        case EQ -> "EQ";
                        case AND -> "AND";
                        case OR -> "OR";
                    });

                    //TODO: support all types
                    //TODO: assert that left.type.equals(right.type)
                    if (tacBinaryOperation.left.type.equals(IcebergType.i32)) {
                        output.append(" i32 ");
                    } else if (tacBinaryOperation.left.type.equals(IcebergType.i64)) {
                        output.append(" i64 ");
                    }

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

                @Override //TODO: remove
                public void visitTacFunction(TacFunction tacFunction) {
                    throw new IllegalStateException("unexpected");
                }

                @Override
                public void visitTacNumber(TacNumber tacNumber) {
                    output.append(tacNumber.value);
                }

                @Override
                public void visitTacPrint(TacPrint tacPrint) {
                    output.append(indent);

                    //TODO: support all types
                    if (tacPrint.argument.type.equals(IcebergType.i32)) {
                        output.append("call void @print_i32(i32 ");
                    } else if (tacPrint.argument.type.equals(IcebergType.i64)) {
                        output.append("call void @print_i64(i64 ");
                    }

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
                public void visitTacVariable(TacVariable tacVariable) {
                    output.append(tacVariable.name);
                }
            }));

            output.append("}");
        });

        return output.toString();
    }
}
