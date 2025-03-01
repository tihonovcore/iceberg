package iceberg.jvm.target;

import iceberg.jvm.cp.Utf8;

import java.util.ArrayList;
import java.util.List;

public class Method {

    public enum AccessFlags {

        ACC_PUBLIC(0x0001),
        ACC_STATIC(0x0008),
        ;

        AccessFlags(int value) {
            this.value = value;
        }

        public final int value;
    }

    public int flags;
    public Utf8 name;
    public Utf8 descriptor;
    public List<CodeAttribute> attributes = new ArrayList<>();
}