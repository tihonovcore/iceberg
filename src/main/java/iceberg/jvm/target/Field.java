package iceberg.jvm.target;

import iceberg.jvm.cp.Utf8;

import java.util.ArrayList;
import java.util.List;

public class Field {

    public enum AccessFlags {

        ACC_PUBLIC(0x0001),
        ;

        AccessFlags(int value) {
            this.value = value;
        }

        public final int value;
    }

    public int flags = AccessFlags.ACC_PUBLIC.value;
    public Utf8 name;
    public Utf8 descriptor;
    public List<Attribute> attributes = new ArrayList<>();
}