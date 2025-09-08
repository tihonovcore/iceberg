package iceberg.jvm.phases.validation;

import iceberg.jvm.target.CodeAttribute;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.OpCodes;
import iceberg.jvm.cp.*;
import iceberg.jvm.target.Method;
import iceberg.jvm.target.StackMapAttribute;
import iceberg.jvm.ir.IcebergType;

import java.util.*;

public class CodegenPrepareStackMapAttributePhase {

    public void execute(CompilationUnit unit) {
        unit.methods.forEach(method -> {
            var attribute = method.attributes.stream()
                .filter(CodeAttribute.class::isInstance)
                .findAny().orElseThrow();

            Map<Integer, Snapshot> snapshots = new HashMap<>();

            var first = new Snapshot();
            if ((method.flags & Method.AccessFlags.ACC_STATIC.value) == 0) {
                first.variables.add(unit.irClass.name);
            }

            //fill local array with parameters
            for (var parameter : attribute.function.parameters) {
                var mapping = Map.of(
                    IcebergType.i32, "int",
                    IcebergType.i64, "long",
                    IcebergType.bool, "boolean",
                    IcebergType.string, "java/lang/String",
                    IcebergType.unit, "void"
                );

                if (mapping.containsKey(parameter.type)) {
                    first.variables.add(mapping.get(parameter.type));
                } else {
                    first.variables.add(parameter.type.irClass.name);
                }
            }

            dfs(attribute.code, 0, unit.constantPool, first, snapshots);

            var stackMapAttribute = new StackMapAttribute(unit.constantPool.computeUtf8("StackMapTable"));
            attribute.attributes.add(stackMapAttribute);

            var keys = snapshots.keySet().stream().sorted().toList();
            for (var i = 1; i < keys.size(); i++) {
                var prev = keys.get(i - 1);
                var curr = keys.get(i);

                var offsetDelta = i == 1 ? curr - prev : curr - prev - 1;
                var full = new StackMapAttribute.FullStackMapFrame(offsetDelta);
                stackMapAttribute.entries.add(full);

                var snapshot = snapshots.get(curr);
                for (var type : snapshot.variables) {
                    if ("int".equals(type) || "boolean".equals(type)) {
                        full.locals.add(new StackMapAttribute.IntegerVariableInfo());
                    } else if ("long".equals(type)) {
                        full.locals.add(new StackMapAttribute.LongVariableInfo());
                    } else {
                        var utf8 = unit.constantPool.computeUtf8(type);
                        var klass = unit.constantPool.computeKlass(utf8);

                        var object = new StackMapAttribute.ObjectVariableInfo(
                            unit.constantPool.indexOf(klass)
                        );
                        full.locals.add(object);
                    }
                }
                for (var type : snapshot.stack) {
                    if ("int".equals(type) || "boolean".equals(type)) {
                        full.stack.add(new StackMapAttribute.IntegerVariableInfo());
                    } else if ("long".equals(type)) {
                        full.stack.add(new StackMapAttribute.LongVariableInfo());
                    } else {
                        var utf8 = unit.constantPool.computeUtf8(type);
                        var klass = unit.constantPool.computeKlass(utf8);

                        var object = new StackMapAttribute.ObjectVariableInfo(
                            unit.constantPool.indexOf(klass)
                        );
                        full.stack.add(object);
                    }
                }
            }
        });
    }

    private static class Snapshot {

        final List<String> variables;
        final List<String> stack;

        public Snapshot() {
            this.variables = new ArrayList<>();
            this.stack = new ArrayList<>();
        }

        public Snapshot(Snapshot other) {
            this.variables = new ArrayList<>(other.variables);
            this.stack = new ArrayList<>(other.stack);
        }

        public void push(String value) {
            stack.add(value);
        }

        public String pop() {
            return stack.removeLast();
        }

        @Override
        public String toString() {
            return "vars: " + variables + ", " + "stack: " + stack;
        }

        public String get(byte index) {
            return variables.get(mapIndex(index));
        }

        public void set(byte index, String type) {
            if (mapIndex(index) == variables.size()) {
                variables.add(type);
            } else {
                variables.set(mapIndex(index), type);
            }
        }

        //every long takes two indexes from array
        private byte mapIndex(byte index) {
            byte current = 0;
            for (byte i = 0; i < variables.size(); i++) {
                if (current == index) {
                    return i;
                }

                if ("long".equals(variables.get(i))) {
                    current += 2;
                } else {
                    current += 1;
                }
            }

            return (byte) variables.size();
        }
    }

    private static final Set<OpCodes> JUMP_OP_CODES = Set.of(
        OpCodes.GOTO, OpCodes.IFEQ, OpCodes.IFNE,
        OpCodes.IF_ICMPEQ, OpCodes.IF_ICMPNE, OpCodes.IF_ICMPLT,
        OpCodes.IF_ICMPLE, OpCodes.IF_ICMPGT, OpCodes.IF_ICMPGE
    );

    private void dfs(
        byte[] code, int i,
        ConstantPool constantPool,
        Snapshot snapshot,
        Map<Integer, Snapshot> snapshots
    ) {
        if (snapshots.containsKey(i)) {
            return;
        } else {
            snapshots.put(i, new Snapshot(snapshot));
        }

        while (i < code.length) {
            var curr = OpCodes.valueOf(code[i]);

            switch (curr) {
                case ALOAD_0 -> snapshot.push(snapshot.get((byte) 0));
                //TODO: not necessary string, better propagate IR to here
                case ACONST_NULL -> snapshot.push("java/lang/String");
                case ICONST_0, ICONST_1, BIPUSH, SIPUSH -> snapshot.push("int");
                case LCONST_0 -> snapshot.push("long");
                case RETURN, IRETURN, ARETURN, LRETURN -> snapshot.stack.clear();
                case DUP -> {
                    var top = snapshot.pop();
                    snapshot.push(top);
                    snapshot.push(top);
                }
                case NEW -> {
                    var index = ((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF);
                    var klass = (Klass) constantPool.load(index);
                    var name = (Utf8) constantPool.load(klass.nameIndex);

                    snapshot.push(new String(name.bytes));
                }
                case GETSTATIC -> {
                    var index = ((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF);
                    snapshot.push(load(constantPool, index).type);
                }
                case GETFIELD -> {
                    snapshot.pop(); //receiver
                    var index = ((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF);
                    snapshot.push(load(constantPool, index).type);
                }
                case PUTFIELD -> {
                    snapshot.pop(); //receiver
                    snapshot.pop(); //value
                }
                case INVOKEVIRTUAL -> {
                    var index = ((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF);
                    var type = (CallableJavaType) load(constantPool, index);

                    snapshot.pop(); //receiver
                    for (int arg = 0; arg < type.arguments.size(); arg++) {
                        snapshot.pop();
                    }

                    if (!type.type.equals("void")) {
                        snapshot.push(type.type);
                    }
                }
                case INVOKESTATIC -> {
                    var index = ((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF);
                    var type = (CallableJavaType) load(constantPool, index);

                    for (int arg = 0; arg < type.arguments.size(); arg++) {
                        snapshot.pop();
                    }

                    if (!type.type.equals("void")) {
                        snapshot.push(type.type);
                    }
                }
                case INVOKESPECIAL -> snapshot.pop();
                case LDC -> snapshot.push(load(constantPool, code[i + 1]).type);
                case LDC_W, LDC_W2 -> {
                    var index = ((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF);
                    var type = load(constantPool, index).type;

                    snapshot.push(type);
                }
                case IFEQ, IFNE -> snapshot.pop();
                case GOTO -> { /* do nothing */ }
                case INEG -> {
                    snapshot.pop();
                    snapshot.push("int");
                }
                case LNEG -> {
                    snapshot.pop();
                    snapshot.push("long");
                }
                case IADD, ISUB, IMUL, IDIV -> {
                    snapshot.pop();
                    snapshot.pop();
                    snapshot.push("int");
                }
                case LADD, LSUB, LMUL, LDIV -> {
                    snapshot.pop();
                    snapshot.pop();
                    snapshot.push("long");
                }
                case I2L -> {
                    snapshot.pop();
                    snapshot.push("long");
                }
                case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPLE, IF_ICMPGT, IF_ICMPGE -> {
                    snapshot.pop();
                    snapshot.pop();
                }
                case LCMP -> {
                    snapshot.pop();
                    snapshot.pop();
                    snapshot.push("int");
                }
                case ISTORE, LSTORE, ASTORE -> {
                    var index = code[i + 1];
                    snapshot.set(index, snapshot.pop());
                }
                case ILOAD, LLOAD, ALOAD -> {
                    var index = code[i + 1];
                    snapshot.push(snapshot.get(index));
                }
                default -> throw new IllegalStateException("not implemented");
            }

            if (JUMP_OP_CODES.contains(curr)) {
                var jump = (short) ((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF);
                dfs(code, i + jump, constantPool, new Snapshot(snapshot), snapshots);
            }

            if (OpCodes.GOTO == curr) {
                return;
            }

            i += switch (curr) {
                case ICONST_0, ICONST_1, LCONST_0 -> 1;
                case ALOAD_0 -> 1;
                case ACONST_NULL -> 1;
                case RETURN, IRETURN, ARETURN, LRETURN -> 1;
                case DUP -> 1;
                case NEW -> 3;
                case GETSTATIC, GETFIELD, PUTFIELD -> 3;
                case INVOKEVIRTUAL, INVOKESTATIC, INVOKESPECIAL -> 3;
                case BIPUSH -> 2;
                case SIPUSH -> 3;
                case LDC -> 2;
                case LDC_W -> 3;
                case LDC_W2 -> 3;
                case I2L -> 1;
                case IADD, ISUB, IMUL, IDIV -> 1;
                case LADD, LSUB, LMUL, LDIV -> 1;
                case INEG, LNEG -> 1;
                case IFEQ, IFNE -> 3;
                case GOTO -> throw new IllegalStateException("Безусловный переход");
                case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPLE, IF_ICMPGT, IF_ICMPGE -> 3;
                case LCMP -> 1;
                case ILOAD, ISTORE, LLOAD, LSTORE, ALOAD, ASTORE -> 2;
            };
        }
    }

    private JavaType load(ConstantPool constantPool, int index) {
        var constant = constantPool.load(index);
        var parser = new JavaDescriptorParser(constantPool);
        return parser.parse(constant);
    }
}
