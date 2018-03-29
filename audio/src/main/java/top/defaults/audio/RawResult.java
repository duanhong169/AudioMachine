package top.defaults.audio;

public class RawResult {
    public String string;
    public int index;
    public boolean end;

    RawResult(String string, int index, boolean end) {
        this.string = string;
        this.index = index;
        this.end = end;
    }
}
