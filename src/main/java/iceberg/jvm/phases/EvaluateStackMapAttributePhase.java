package iceberg.jvm.phases;

import iceberg.antlr.IcebergParser;
import iceberg.jvm.CompilationUnit;
import iceberg.jvm.OpCodes;
import iceberg.jvm.cp.*;
import iceberg.jvm.target.StackMapAttribute;

import java.util.*;

public class EvaluateStackMapAttributePhase implements CompilationPhase {

    @Override
    public void execute(IcebergParser.FileContext file, CompilationUnit unit) {
        unit.methods.forEach(method -> {
            var attribute = method.attributes.stream()
                .filter(CompilationUnit.CodeAttribute.class::isInstance)
                .findAny().orElseThrow();

            Map<Integer, Snapshot> snapshots = new HashMap<>();

            var first = new Snapshot();
            if ((method.flags & CompilationUnit.Method.AccessFlags.ACC_STATIC.value) == 0) {
                first.variables.add("this");
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
                //TODO: fill vars
                for (var type : snapshot.stack) {
                    if ("int".equals(type)) {
                        full.stack.add(new StackMapAttribute.IntegerVariableInfo());
                    //TODO: other types
                    } else { //class
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
        List<String> variables;
        List<String> stack;

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

        public void pop() {
            stack.remove(stack.size() - 1);
        }

        @Override
        public String toString() {
            return "vars: " + variables + ", " + "stack: " + stack;
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
                case ICONST_0 -> snapshot.push("int");
                case ICONST_1 -> snapshot.push("int");
                case ALOAD_0 -> snapshot.push(snapshot.variables.get(0));
                case RETURN -> snapshot.stack.clear();
                case GETSTATIC -> {
                    var index = ((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF);
                    snapshot.push(load(constantPool, index));
                }
                case INVOKEVIRTUAL -> {
                    snapshot.pop();
                    snapshot.pop(); //NOTE: now it pops argument of System.out.println
                    //TODO: pop arguments
                }
                case INVOKESPECIAL -> {
                    snapshot.pop();
                    //TODO: pop arguments
                }
                case BIPUSH -> snapshot.push("int");
                case SIPUSH -> snapshot.push("int");
                case LDC -> {
                    snapshot.push(load(constantPool, code[i + 1]));
                }
                case LDC_W, LDC_W2 -> {
                    var index = ((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF);
                    snapshot.push(load(constantPool, index));
                }
                case IFEQ -> snapshot.pop();
                case IFNE -> snapshot.pop();
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
                default -> throw new IllegalStateException("not implemented");
            }

            if (JUMP_OP_CODES.contains(curr)) {
                var jump = ((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF);
                dfs(code, i + jump, constantPool, new Snapshot(snapshot), snapshots);
            }

            if (OpCodes.GOTO == curr) {
                return;
            }

            i += switch (curr) {
                case ICONST_0 -> 1;
                case ICONST_1 -> 1;
                case ALOAD_0 -> 1;
                case RETURN -> 1;
                case GETSTATIC -> 3;
                case INVOKEVIRTUAL -> 3;
                case INVOKESPECIAL -> 3;
                case BIPUSH -> 2;
                case SIPUSH -> 3;
                case LDC -> 2;
                case LDC_W -> 3;
                case LDC_W2 -> 3;
                case I2L -> 1;
                case IADD, ISUB, IMUL, IDIV -> 1;
                case LADD, LSUB, LMUL, LDIV -> 1;
                case INEG -> 1;
                case LNEG -> 1;
                case IFEQ -> 3;
                case IFNE -> 3;
                case GOTO -> throw new IllegalStateException("Безусловный переход");
                case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPLE, IF_ICMPGT, IF_ICMPGE -> 3;
                case LCMP -> 1;
            };
        }
    }

    private String load(ConstantPool constantPool, int index) {
        var constant = constantPool.load(index);
        if (constant instanceof FieldRef ref) {
            var nameAndType = (NameAndType) constantPool.load(ref.nameAndTypeIndex);
            var utf8 = (Utf8) constantPool.load(nameAndType.descriptorIndex);
            var typeDescriptor = new String(utf8.bytes);

            //TODO: decode descriptor - for int it will be I (not int)
            return typeDescriptor.substring(1, typeDescriptor.length() - 1);
        }

        if (constant instanceof IntegerInfo) {
            return "int";
        }

        if (constant instanceof LongInfo) {
            return "long";
        }

        if (constant instanceof StringInfo) {
            return "java/lang/String";
        }

        throw new IllegalStateException();
    }
}
