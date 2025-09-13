package iceberg.llvm.opt.cp;

import iceberg.llvm.tac.TacNumber;

sealed interface Value {

    UnDef UNDEF = new UnDef();
    OverDef OVERDEF = new OverDef();

    Value join(Value other);

    final class Const implements Value {
        final TacNumber value;

        public Const(TacNumber value) {
            this.value = value;
        }

        @Override
        public Value join(Value other) {
            return switch (other) {
                case UnDef __ -> this;
                case OverDef __ -> other;
                case Const c -> value.value == c.value.value ? this : Value.OVERDEF;
            };
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    final class UnDef implements Value {
        @Override
        public Value join(Value other) {
            return other;
        }

        @Override
        public String toString() {
            return "UNDEF";
        }
    }

    final class OverDef implements Value {
        @Override
        public Value join(Value other) {
            return this;
        }

        @Override
        public String toString() {
            return "OVERDEF";
        }
    }
}
