package top.defaults.audio;

import java.util.concurrent.Callable;

public interface AudioProcessorDelegate {

    void initialize();

    void release();

    Callable<RawResult> compose(int index, byte[] buffer, int length, boolean end);

    int packageSize();

}
