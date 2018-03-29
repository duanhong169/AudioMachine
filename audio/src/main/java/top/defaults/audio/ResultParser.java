package top.defaults.audio;

public interface ResultParser<T> {

    T parse(RawResult rawResult);
}
