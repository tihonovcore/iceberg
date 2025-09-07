package iceberg.jvm.phases;

import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.target.Field;

public class CodegenPrepareFieldsPhase {

    public void execute(CompilationUnit unit) {
        for (var entry : unit.irClass.fields.entrySet()) {
            var field = new Field();
            field.flags = Field.AccessFlags.ACC_PUBLIC.value;
            field.name = unit.constantPool.computeUtf8(entry.getKey());
            field.descriptor = unit.constantPool.computeUtf8(entry.getValue().javaFieldDescriptor());

            unit.fields.add(field);
        }
    }
}
