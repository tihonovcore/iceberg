package iceberg.jvm.target;

import iceberg.jvm.cp.Utf8;

public class SourceAttribute implements Attribute {

    public final Utf8 attributeName;
    public final Utf8 sourceFileName;

    public SourceAttribute(Utf8 attributeName, Utf8 sourceFileName) {
        this.attributeName = attributeName;
        this.sourceFileName = sourceFileName;
    }
}
