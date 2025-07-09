package iceberg.jvm.phases;

import iceberg.jvm.ir.IrFile;
import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.target.SourceAttribute;

import java.util.List;

public class MoveEachClassToSeparateUnitPhase {

    public List<CompilationUnit> execute(IrFile irFile) {
        return irFile.classes.stream().map(irClass -> {
            var unit = new CompilationUnit();

            var utf8 = unit.constantPool.computeUtf8(irClass.name);
            unit.thisRef = unit.constantPool.computeKlass(utf8);

            unit.attributes.add(new SourceAttribute(
                unit.constantPool.computeUtf8("SourceFile"),
                unit.constantPool.computeUtf8(irClass.name + ".ib")
            ));

            unit.irClass = irClass;

            return unit;
        }).toList();
    }
}
