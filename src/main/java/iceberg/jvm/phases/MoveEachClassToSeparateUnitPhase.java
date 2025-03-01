package iceberg.jvm.phases;

import iceberg.jvm.target.CompilationUnit;
import iceberg.jvm.target.SourceAttribute;

import java.util.List;

public class MoveEachClassToSeparateUnitPhase {

    public void execute(CompilationUnit main, List<CompilationUnit> units) {
        main.irFile.classes.forEach(irClass -> {
            var unit = new CompilationUnit();

            var utf8 = unit.constantPool.computeUtf8(irClass.name);
            unit.thisRef = unit.constantPool.computeKlass(utf8);

            unit.attributes.add(new SourceAttribute(
                unit.constantPool.computeUtf8("SourceFile"),
                unit.constantPool.computeUtf8(irClass.name + ".ib")
            ));

            unit.irClass = irClass;

            units.add(unit);
        });
    }
}
