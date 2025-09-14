package iceberg.pg;

@SuppressWarnings("unused")
public class Reader {

    public int i32(int i) {
        return Integer.parseInt(string(i));
    }

    public long i64(int i) {
        return Long.parseLong(string(i));
    }

    public boolean bool(int i) {
        return Boolean.parseBoolean(string(i));
    }

    public String string(int i) {
        return System.getenv("ARG" + i);
    }
}
