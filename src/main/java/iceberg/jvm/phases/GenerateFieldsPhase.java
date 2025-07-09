package iceberg.jvm.phases;

import iceberg.jvm.ir.IcebergType;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.target.Field;

public class GenerateFieldsPhase {

    public void execute(CompilationUnit unit) {
        for (var entry : unit.irClass.fields.entrySet()) {
            var field = new Field();
            field.flags = Field.AccessFlags.ACC_PUBLIC.value;
            field.name = unit.constantPool.computeUtf8(entry.getKey());

            //TODO: support all types
            if (entry.getValue().type != IcebergType.i32) {
                throw new IllegalStateException("type not yet supported");
            }
            field.descriptor = unit.constantPool.computeUtf8("I");

            unit.fields.add(field);
        }
    }
}
