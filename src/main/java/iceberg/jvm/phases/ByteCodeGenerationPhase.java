package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.ByteArray;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.OpCodes;
import iceberg.jvm.ir.*;

public class ByteCodeGenerationPhase implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        unit.methods.forEach(method -> {
            var attribute = method.attributes.stream()
                .filter(CompilationUnit.CodeAttribute.class::isInstance)
                .findAny().orElseThrow();
            generateBytecode(attribute, unit);
        });
    }

    private void generateBytecode(
        CompilationUnit.CodeAttribute attribute,
        CompilationUnit compilationUnit
    ) {
        var output = new ByteArray();
        attribute.body.accept(new IrVisitor() {
            @Override
            public void visitIrBody(IrBody irBody) {
                irBody.statements.forEach(s -> s.accept(this));
            }

            @Override
            public void visitIrSuperCall(IrSuperCall irSuperCall) {
                output.writeU1(OpCodes.ALOAD_0.value);
                output.writeU1(OpCodes.INVOKESPECIAL.value);
                output.writeU2(compilationUnit.constantPool.indexOf(irSuperCall.methodRef));
            }

            @Override
            public void visitIrReturn(IrReturn irReturn) {
                output.writeU1(OpCodes.RETURN.value);
            }

            @Override
            public void visitIrUnaryExpression(IrUnaryExpression irExpression) {
                switch (irExpression.operator) {
                    case NOT -> {
                        irExpression.value.accept(this);
                        output.writeU1(OpCodes.IFEQ.value);
                        var toTrue = output.lateInitJump();

                        output.writeU1(OpCodes.ICONST_0.value);
                        output.writeU1(OpCodes.GOTO.value);
                        var toEnd = output.lateInitJump();

                        toTrue.jump();
                        output.writeU1(OpCodes.ICONST_1.value);

                        toEnd.jump();
                    }
                    case MINUS -> {
                        irExpression.value.accept(this);
                        switch (irExpression.type) {
                            case i32 -> output.writeU1(OpCodes.INEG.value);
                            case i64 -> output.writeU1(OpCodes.LNEG.value);
                        }
                    }
                }
            }

            @Override
            public void visitIrBinaryExpression(IrBinaryExpression irExpression) {
                switch (irExpression.operator) {
                    case OR -> {
                        irExpression.left.accept(this);
                        output.writeU1(OpCodes.IFEQ.value);
                        var toElse = output.lateInitJump();

                        output.writeU1(OpCodes.ICONST_1.value);
                        output.writeU1(OpCodes.GOTO.value);
                        var toAfterIf = output.lateInitJump();

                        toElse.jump();
                        irExpression.right.accept(this);
                        toAfterIf.jump();

                        return;
                    }
                    case AND -> {
                        irExpression.left.accept(this);
                        output.writeU1(OpCodes.IFEQ.value);
                        var toElse = output.lateInitJump();

                        irExpression.right.accept(this);
                        output.writeU1(OpCodes.GOTO.value);
                        var toAfterIf = output.lateInitJump();

                        toElse.jump();
                        output.writeU1(OpCodes.ICONST_0.value);
                        toAfterIf.jump();

                        return;
                    }
                }

                irExpression.left.accept(this);
                if (irExpression.left.type != irExpression.right.type) {
                    if (irExpression.left.type == IcebergType.i32) {
                        output.writeU1(OpCodes.I2L.value);
                    }
                }

                irExpression.right.accept(this);
                if (irExpression.left.type != irExpression.right.type) {
                    if (irExpression.right.type == IcebergType.i32) {
                        output.writeU1(OpCodes.I2L.value);
                    }
                }

                switch (irExpression.operator) {
                    case PLUS -> {
                        switch (irExpression.type) {
                            case i32 -> output.writeU1(OpCodes.IADD.value);
                            case i64 -> output.writeU1(OpCodes.LADD.value);
                        }
                    }
                    case SUB -> {
                        switch (irExpression.type) {
                            case i32 -> output.writeU1(OpCodes.ISUB.value);
                            case i64 -> output.writeU1(OpCodes.LSUB.value);
                        }
                    }
                    case MULT -> {
                        switch (irExpression.type) {
                            case i32 -> output.writeU1(OpCodes.IMUL.value);
                            case i64 -> output.writeU1(OpCodes.LMUL.value);
                        }
                    }
                    case DIV -> {
                        switch (irExpression.type) {
                            case i32 -> output.writeU1(OpCodes.IDIV.value);
                            case i64 -> output.writeU1(OpCodes.LDIV.value);
                        }
                    }
                    case LE -> {
                        if (irExpression.left.type == IcebergType.i64 || irExpression.right.type == IcebergType.i64) {
                            output.writeU1(OpCodes.LCMP.value);
                            output.writeU1(OpCodes.ICONST_0.value);
                        }

                        output.writeU1(OpCodes.IF_ICMPLE.value);
                        var toTrue = output.lateInitJump();
                        output.writeU1(OpCodes.ICONST_0.value);
                        output.writeU1(OpCodes.GOTO.value);
                        var toEnd = output.lateInitJump();

                        toTrue.jump();
                        output.writeU1(OpCodes.ICONST_1.value);
                        toEnd.jump();
                    }
                    case LT -> {
                        if (irExpression.left.type == IcebergType.i64 || irExpression.right.type == IcebergType.i64) {
                            output.writeU1(OpCodes.LCMP.value);
                            output.writeU1(OpCodes.ICONST_0.value);
                        }

                        output.writeU1(OpCodes.IF_ICMPLT.value);
                        var toTrue = output.lateInitJump();
                        output.writeU1(OpCodes.ICONST_0.value);
                        output.writeU1(OpCodes.GOTO.value);
                        var toEnd = output.lateInitJump();

                        toTrue.jump();
                        output.writeU1(OpCodes.ICONST_1.value);
                        toEnd.jump();
                    }
                    case EQ -> {
                        if (irExpression.left.type == IcebergType.i64 || irExpression.right.type == IcebergType.i64) {
                            output.writeU1(OpCodes.LCMP.value);
                            output.writeU1(OpCodes.ICONST_0.value);
                        }

                        output.writeU1(OpCodes.IF_ICMPEQ.value);
                        var toTrue = output.lateInitJump();
                        output.writeU1(OpCodes.ICONST_0.value);
                        output.writeU1(OpCodes.GOTO.value);
                        var toEnd = output.lateInitJump();

                        toTrue.jump();
                        output.writeU1(OpCodes.ICONST_1.value);
                        toEnd.jump();
                    }
                }
            }

            @Override
            public void visitIrNumber(IrNumber irNumber) {
                var value = irNumber.value;
                if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
                    output.writeU1(OpCodes.BIPUSH.value);
                    output.writeU1((int) value);
                } else if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE) {
                    output.writeU1(OpCodes.SIPUSH.value);
                    output.writeU2((int) value);
                } else if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
                    var indexInPool = compilationUnit.constantPool.findInteger((int) value);
                    if (Byte.MIN_VALUE <= indexInPool && indexInPool <= Byte.MAX_VALUE) {
                        output.writeU1(OpCodes.LDC.value);
                        output.writeU1(indexInPool);
                    } else {
                        output.writeU1(OpCodes.LDC_W.value);
                        output.writeU2(indexInPool);
                    }
                } else {
                    var indexInPool = compilationUnit.constantPool.findLong(value);
                    output.writeU1(OpCodes.LDC_W2.value);
                    output.writeU2(indexInPool);
                }
            }

            @Override
            public void visitIrBool(IrBool irBool) {
                if (irBool.value) {
                    output.writeU1(OpCodes.ICONST_1.value);
                } else {
                    output.writeU1(OpCodes.ICONST_0.value);
                }
            }

            @Override
            public void visitIrString(IrString irString) {
                var indexInPool = compilationUnit.constantPool.indexOf(irString.value);
                if (Byte.MIN_VALUE <= indexInPool && indexInPool <= Byte.MAX_VALUE) {
                    output.writeU1(OpCodes.LDC.value);
                    output.writeU1(indexInPool);
                } else {
                    output.writeU1(OpCodes.LDC_W.value);
                    output.writeU2(indexInPool);
                }
            }

            @Override
            public void visitIrStaticCall(IrStaticCall irStaticCall) {
                output.writeU1(OpCodes.GETSTATIC.value);
                output.writeU2(compilationUnit.constantPool.indexOf(irStaticCall.fieldRef));

                irStaticCall.arguments.forEach(e -> e.accept(this));

                output.writeU1(OpCodes.INVOKEVIRTUAL.value);
                output.writeU2(compilationUnit.constantPool.indexOf(irStaticCall.methodRef));
            }
        });

        attribute.code = output.bytes();
    }
}
