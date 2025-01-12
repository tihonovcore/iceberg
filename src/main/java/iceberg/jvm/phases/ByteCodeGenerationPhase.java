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
        });;
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
            public void visitIrBinaryExpression(IrBinaryExpression irExpression) {
                irExpression.left.accept(this);
                irExpression.right.accept(this);
                switch (irExpression.operator) {
                    case OR -> output.writeU1(OpCodes.IOR.value);
                    case AND -> output.writeU1(OpCodes.IAND.value);
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
