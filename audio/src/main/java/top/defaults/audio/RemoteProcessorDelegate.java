package top.defaults.audio;

import java.util.Locale;
import java.util.concurrent.Callable;

public class RemoteProcessorDelegate implements AudioProcessorDelegate {
    @Override
    public void initialize() {}

    @Override
    public void release() {}

    @Override
    public Callable<RawResult> compose(final int index, byte[] buffer, int length, final boolean end) {
        return new Callable<RawResult>() {
            @Override
            public RawResult call() throws Exception {
                return new RawResult(String.format(Locale.getDefault(), "mock %d %s", index, end), index, end);
            }
        };
    }

    @Override
    public int packageSize() {
        return 4000;
    }
}
