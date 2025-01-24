package iceberg.jvm.target;

import iceberg.jvm.CompilationUnit;
import iceberg.jvm.cp.Utf8;

import java.util.ArrayList;
import java.util.List;

public class StackMapAttribute implements CompilationUnit.Attribute {

    public final Utf8 attributeName;
    public final List<FullStackMapFrame> entries;

    public StackMapAttribute(Utf8 attributeName) {
        this.attributeName = attributeName;
        this.entries = new ArrayList<>();
    }

    public static class FullStackMapFrame {

        public final int frameType = 255;
        public final int offsetDelta;
        public final List<VerificationTypeInfo> locals = new ArrayList<>();
        public final List<VerificationTypeInfo> stack = new ArrayList<>();

        public FullStackMapFrame(int offsetDelta) {
            this.offsetDelta = offsetDelta;
        }
    }

    public static abstract class VerificationTypeInfo {

        public abstract int tag();
    }

    public static class IntegerVariableInfo extends VerificationTypeInfo {

        @Override
        public int tag() {
            return 1;
        }
    }

    public static class LongVariableInfo extends VerificationTypeInfo {

        @Override
        public int tag() {
            return 4;
        }
    }

    public static class ObjectVariableInfo extends VerificationTypeInfo {

        public final int cpoolIndex;

        public ObjectVariableInfo(int cpoolIndex) {
            this.cpoolIndex = cpoolIndex;
        }

        @Override
        public int tag() {
            return 7;
        }
    }
}
