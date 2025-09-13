package iceberg.llvm.opt.cp;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class VariablesVector {

    final Map<String, Value> values;

    VariablesVector(Map<String, Value> values) {
        this.values = values;
    }

    public VariablesVector copy() {
        return new VariablesVector(
            values.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), values::get))
        );
    }

    public VariablesVector join(VariablesVector other) {
        return new VariablesVector(
            values.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), key -> {
                    var a = values.get(key);
                    var b = other.values.get(key);
                    return a.join(b);
                }))
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VariablesVector other) {
            return values.keySet().stream()
                .allMatch(key -> {
                    var a = values.get(key);
                    var b = other.values.get(key);

                    return a == Value.UNDEF && b == Value.UNDEF
                        || a == Value.OVERDEF && b == Value.OVERDEF
                        || a instanceof Value.Const ac
                        && b instanceof Value.Const bc
                        && ac.value.value == bc.value.value;
                });
        }

        return false;
    }
}
