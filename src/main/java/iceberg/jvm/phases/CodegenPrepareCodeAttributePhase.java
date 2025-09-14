package iceberg.jvm.phases;

import iceberg.ir.*;
import iceberg.jvm.ByteArray;
import iceberg.jvm.cp.MethodRef;
import iceberg.jvm.target.CodeAttribute;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.OpCodes;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CodegenPrepareCodeAttributePhase {

    public void execute(CompilationUnit unit) {
        unit.methods.forEach(method -> {
            var attribute = method.attributes.stream()
                .filter(CodeAttribute.class::isInstance)
                .findAny().orElseThrow();
            generateBytecode(attribute, unit);
        });
    }

    private void generateBytecode(
        CodeAttribute attribute,
        CompilationUnit compilationUnit
    ) {
        var output = new ByteArray();
        attribute.function.accept(new IrVisitor() {

            private final List<Set<IrVariable>> scopes = new ArrayList<>();

            @Override
            public void visitIrFile(IrFile irFile) {
                throw new IllegalStateException("unexpected IrFile");
            }

            @Override
            public void visitIrClass(IrClass irClass) {
                throw new IllegalStateException("unexpected IrClass");
            }

            @Override
            public void visitIrNew(IrNew irNew) {
                var klass = compilationUnit.constantPool.computeKlass(
                    compilationUnit.constantPool.computeUtf8(irNew.irClass.name)
                );
                var klassIndex = compilationUnit.constantPool.indexOf(klass);

                output.writeU1(OpCodes.NEW.value);
                output.writeU2(klassIndex);

                var methodRef = computeMethodRef(irNew.irClass.findDefaultConstructor());
                var methodRefIndex = compilationUnit.constantPool.indexOf(methodRef);

                output.writeU1(OpCodes.DUP.value);
                output.writeU1(OpCodes.INVOKESPECIAL.value);
                output.writeU2(methodRefIndex);
            }

            @Override
            public void visitIrFunction(IrFunction irFunction) {
                if (!irFunction.isJavaStatic()) {
                    var type = new IcebergType(irFunction.irClass);
                    var thisParameter = new IrVariable(type,null);
                    addToLocalVariables(thisParameter);
                }

                for (var parameter : irFunction.parameters) {
                    addToLocalVariables(parameter);
                }
                irFunction.irBody.accept(this);
            }

            @Override
            public void visitIrBody(IrBody irBody) {
                scopes.add(new HashSet<>());

                irBody.statements.forEach(statement -> {
                    statement.accept(this);
                    dropUnusedReturnValue(statement);
                });

                scopes.removeLast().forEach(irVariable -> {
                    indexes.remove(irVariable);

                    if (irVariable.type == IcebergType.i64) {
                        var longPlaceholder = indexes.keySet().stream()
                            .filter(LongPlaceholder.class::isInstance)
                            .findFirst().orElseThrow();
                        indexes.remove(longPlaceholder);
                    }
                });
            }

            private void dropUnusedReturnValue(IR statement) {
                if (statement instanceof IrMethodCall call) {
                    if (call.function.returnType.equals(IcebergType.unit)) {
                        return;
                    }

                    output.writeU1(OpCodes.POP.value);
                }

                if (statement instanceof IrStaticCall call) {
                    if (call.function.returnType.equals(IcebergType.unit)) {
                        return;
                    }

                    output.writeU1(OpCodes.POP.value);
                }
            }

            @Override
            public void visitIrLoop(IrLoop irLoop) {
                //HACK:
                //If there is variable declaration inside while-loop, we have to forget about
                //this variable after jump back to condition. Unfortunately, there is no information
                //about scopes at EvaluateStackMapAttributePhase, so it keeps the variable on stack
                //after jump and then captures frame-snapshot.
                //This GOTO captures frame-snapshot BEFORE visiting body, without any body-vars.
                output.writeU1(OpCodes.GOTO.value);
                output.lateInitJump().jump();

                var beforeCondition = output.length();

                irLoop.condition.accept(this);
                output.writeU1(OpCodes.IFEQ.value);
                var afterLoop = output.lateInitJump();

                irLoop.body.accept(this);
                output.writeU1(OpCodes.GOTO.value);
                output.writeU2((short) (beforeCondition - output.length() + 1));

                afterLoop.jump();
            }

            @Override
            public void visitIrSuperCall(IrSuperCall irSuperCall) {
                output.writeU1(OpCodes.ALOAD_0.value);

                var methodRef = computeMethodRef(irSuperCall.function);
                var index = compilationUnit.constantPool.indexOf(methodRef);

                output.writeU1(OpCodes.INVOKESPECIAL.value);
                output.writeU2(index);
            }

            @Override
            public void visitIrReturn(IrReturn irReturn) {
                if (irReturn.expression == null) {
                    output.writeU1(OpCodes.RETURN.value);
                    return;
                }

                irReturn.expression.accept(this);

                var type = irReturn.expression.type;
                if (type == IcebergType.i32 || type == IcebergType.bool) {
                    output.writeU1(OpCodes.IRETURN.value);
                } else if (type == IcebergType.i64) {
                    output.writeU1(OpCodes.LRETURN.value);
                } else {
                    output.writeU1(OpCodes.ARETURN.value);
                }
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
                        if (irExpression.type == IcebergType.i32) {
                            output.writeU1(OpCodes.INEG.value);
                        } else if (irExpression.type == IcebergType.i64) {
                            output.writeU1(OpCodes.LNEG.value);
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
                irExpression.right.accept(this);

                switch (irExpression.operator) {
                    case PLUS -> {
                        if (irExpression.type == IcebergType.i32) {
                            output.writeU1(OpCodes.IADD.value);
                        } else if (irExpression.type == IcebergType.i64) {
                            output.writeU1(OpCodes.LADD.value);
                        }
                    }
                    case SUB -> {
                        if (irExpression.type == IcebergType.i32) {
                            output.writeU1(OpCodes.ISUB.value);
                        } else if (irExpression.type == IcebergType.i64) {
                            output.writeU1(OpCodes.LSUB.value);
                        }
                    }
                    case MULT -> {
                        if (irExpression.type == IcebergType.i32) {
                            output.writeU1(OpCodes.IMUL.value);
                        } else if (irExpression.type == IcebergType.i64) {
                            output.writeU1(OpCodes.LMUL.value);
                        }
                    }
                    case DIV -> {
                        if (irExpression.type == IcebergType.i32) {
                            output.writeU1(OpCodes.IDIV.value);
                        } else if (irExpression.type == IcebergType.i64) {
                            output.writeU1(OpCodes.LDIV.value);
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
            public void visitIrCast(IrCast irCast) {
                if (irCast.type == IcebergType.i64 && irCast.irExpression.type == IcebergType.i32) {
                    irCast.irExpression.accept(this);
                    output.writeU1(OpCodes.I2L.value);
                } else {
                    throw new IllegalStateException();
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
                    var constant = compilationUnit.constantPool.computeInt((int) value);
                    var indexInPool = compilationUnit.constantPool.indexOf(constant);
                    if (Byte.MIN_VALUE <= indexInPool && indexInPool <= Byte.MAX_VALUE) {
                        output.writeU1(OpCodes.LDC.value);
                        output.writeU1(indexInPool);
                    } else {
                        output.writeU1(OpCodes.LDC_W.value);
                        output.writeU2(indexInPool);
                    }
                } else {
                    var constant = compilationUnit.constantPool.computeLong(value);
                    var indexInPool = compilationUnit.constantPool.indexOf(constant);
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
                var constant = compilationUnit.constantPool.computeString(irString.value);
                var indexInPool = compilationUnit.constantPool.indexOf(constant);
                if (Byte.MIN_VALUE <= indexInPool && indexInPool <= Byte.MAX_VALUE) {
                    output.writeU1(OpCodes.LDC.value);
                    output.writeU1(indexInPool);
                } else {
                    output.writeU1(OpCodes.LDC_W.value);
                    output.writeU2(indexInPool);
                }
            }

            @Override
            public void visitIrPrint(IrPrint irPrint) {
                var systemClass = compilationUnit.constantPool.computeKlass(
                    compilationUnit.constantPool.computeUtf8("java/lang/System")
                );
                var outField = compilationUnit.constantPool.computeNameAndType(
                    compilationUnit.constantPool.computeUtf8("out"),
                    compilationUnit.constantPool.computeUtf8("Ljava/io/PrintStream;")
                );
                var fieldRef = compilationUnit.constantPool.computeFieldRef(systemClass, outField);

                output.writeU1(OpCodes.GETSTATIC.value);
                output.writeU2(compilationUnit.constantPool.indexOf(fieldRef));

                irPrint.argument.accept(this);

                var methodRef = computeMethodRef(irPrint.function);
                var index = compilationUnit.constantPool.indexOf(methodRef);

                output.writeU1(OpCodes.INVOKEVIRTUAL.value);
                output.writeU2(index);
            }

            @Override
            public void visitIrStaticCall(IrStaticCall irStaticCall) {
                irStaticCall.arguments.forEach(e -> e.accept(this));

                var methodRef = computeMethodRef(irStaticCall.function);
                var index = compilationUnit.constantPool.indexOf(methodRef);

                output.writeU1(OpCodes.INVOKESTATIC.value);
                output.writeU2(index);
            }

            @Override
            public void visitIrMethodCall(IrMethodCall irMethodCall) {
                irMethodCall.receiver.accept(this);
                irMethodCall.arguments.forEach(e -> e.accept(this));

                var methodRef = computeMethodRef(irMethodCall.function);
                var index = compilationUnit.constantPool.indexOf(methodRef);

                output.writeU1(OpCodes.INVOKEVIRTUAL.value);
                output.writeU2(index);
            }

            private MethodRef computeMethodRef(IrFunction irFunction) {
                var klass = compilationUnit.constantPool.computeKlass(
                    compilationUnit.constantPool.computeUtf8(irFunction.irClass.name)
                );
                var method = compilationUnit.constantPool.computeNameAndType(
                    compilationUnit.constantPool.computeUtf8(irFunction.name),
                    compilationUnit.constantPool.computeUtf8(irFunction.javaMethodDescriptor())
                );

                return compilationUnit.constantPool.computeMethodRef(klass, method);
            }

            private final Map<IrVariable, Integer> indexes = new HashMap<>();

            private int addToLocalVariables(IrVariable irVariable) {
                int index = indexes.computeIfAbsent(irVariable, __ -> indexes.size());

                if (irVariable.type == IcebergType.i64) {
                    indexes.put(new LongPlaceholder(IcebergType.i64, null), indexes.size());
                }

                return index;
            }

            @Override
            public void visitIrVariable(IrVariable irVariable) {
                if (irVariable.initializer != null) {
                    irVariable.initializer.accept(this);
                } else {
                    if (irVariable.type == IcebergType.i32 || irVariable.type == IcebergType.bool) {
                        output.writeU1(OpCodes.ICONST_0.value);
                    } else if (irVariable.type == IcebergType.i64) {
                        output.writeU1(OpCodes.LCONST_0.value);
                    } else {
                        output.writeU1(OpCodes.ACONST_NULL.value);
                    }
                }

                if (irVariable.type == IcebergType.i32 || irVariable.type == IcebergType.bool) {
                    output.writeU1(OpCodes.ISTORE.value);
                } else if (irVariable.type == IcebergType.i64) {
                    output.writeU1(OpCodes.LSTORE.value);
                } else {
                    output.writeU1(OpCodes.ASTORE.value);
                }

                var index = addToLocalVariables(irVariable);
                output.writeU1(index);

                scopes.getLast().add(irVariable);
            }

            static class LongPlaceholder extends IrVariable {

                public LongPlaceholder(IcebergType type, @Nullable IrExpression initializer) {
                    super(type, initializer);
                }
            }

            @Override
            public void visitIrReadVariable(IrReadVariable irReadVariable) {
                var type = irReadVariable.definition.type;
                if (type == IcebergType.i32 || type == IcebergType.bool) {
                    output.writeU1(OpCodes.ILOAD.value);
                } else if (type == IcebergType.i64) {
                    output.writeU1(OpCodes.LLOAD.value);
                } else {
                    output.writeU1(OpCodes.ALOAD.value);
                }

                var index = indexes.get(irReadVariable.definition);
                output.writeU1(index);
            }

            @Override
            public void visitIrAssignVariable(IrAssignVariable irAssignVariable) {
                irAssignVariable.expression.accept(this);

                var type = irAssignVariable.definition.type;
                if (type == IcebergType.i32 || type == IcebergType.bool) {
                    output.writeU1(OpCodes.ISTORE.value);
                } else if (type == IcebergType.i64) {
                    output.writeU1(OpCodes.LSTORE.value);
                } else {
                    output.writeU1(OpCodes.ASTORE.value);
                }

                var index = indexes.get(irAssignVariable.definition);
                output.writeU1(index);
            }

            @Override
            public void visitIrField(IrField irField) {
                //do nothing
            }

            @Override
            public void visitIrGetField(IrGetField irGetField) {
                irGetField.receiver.accept(this);

                output.writeU1(OpCodes.GETFIELD.value);

                var irField = irGetField.irField;
                var klass = compilationUnit.constantPool.computeKlass(
                    compilationUnit.constantPool.computeUtf8(irField.irClass.name)
                );

                var field = compilationUnit.constantPool.computeNameAndType(
                    compilationUnit.constantPool.computeUtf8(irField.fieldName),
                    compilationUnit.constantPool.computeUtf8(irField.javaFieldDescriptor())
                );

                var fieldRef = compilationUnit.constantPool.computeFieldRef(klass, field);
                output.writeU2(compilationUnit.constantPool.indexOf(fieldRef));
            }

            @Override
            public void visitIrPutField(IrPutField irPutField) {
                irPutField.receiver.accept(this);
                irPutField.expression.accept(this);

                output.writeU1(OpCodes.PUTFIELD.value);

                var irField = irPutField.irField;
                var klass = compilationUnit.constantPool.computeKlass(
                    compilationUnit.constantPool.computeUtf8(irField.irClass.name)
                );

                var field = compilationUnit.constantPool.computeNameAndType(
                    compilationUnit.constantPool.computeUtf8(irField.fieldName),
                    compilationUnit.constantPool.computeUtf8(irField.javaFieldDescriptor())
                );

                var fieldRef = compilationUnit.constantPool.computeFieldRef(klass, field);
                output.writeU2(compilationUnit.constantPool.indexOf(fieldRef));
            }

            @Override
            public void visitIrIfStatement(IrIfStatement irIfStatement) {
                irIfStatement.condition.accept(this);

                output.writeU1(OpCodes.IFEQ.value);
                var toElseOrEnd = output.lateInitJump();

                irIfStatement.thenStatement.accept(this);

                if (irIfStatement.elseStatement == null) {
                    toElseOrEnd.jump();
                    return;
                }

                if (thenBlockEndsWithReturn(irIfStatement)) {
                    toElseOrEnd.jump();
                    irIfStatement.elseStatement.accept(this);
                } else {
                    output.writeU1(OpCodes.GOTO.value);
                    var toEnd = output.lateInitJump();

                    toElseOrEnd.jump();
                    irIfStatement.elseStatement.accept(this);
                    toEnd.jump();
                }
            }

            private boolean thenBlockEndsWithReturn(IrIfStatement irIfStatement) {
                if (irIfStatement.thenStatement instanceof IrReturn) {
                    return true;
                }

                if (irIfStatement.thenStatement instanceof IrBody irBody) {
                    return !irBody.statements.isEmpty() && irBody.statements.getLast() instanceof IrReturn;
                }

                return false;
            }

            @Override
            public void visitIrThis(IrThis irThis) {
                output.writeU1(OpCodes.ALOAD_0.value);
            }
        });

        attribute.code = output.bytes();
    }
}
