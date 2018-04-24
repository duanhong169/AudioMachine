package top.defaults.audio;

import java.util.Locale;
import java.util.concurrent.Callable;

class MockProcessorDelegate implements AudioProcessorDelegate {

    private int packageSize = Constants.DEFAULT_PROCESSOR_PACKAGE_SIZE;
    private int threadCount = Constants.DEFAULT_PROCESSOR_THREAD_COUNT;

    @Override
    public void initialize() {}

    @Override
    public void release() {}

    @Override
    public Callable<RawResult> compose(final int index, byte[] buffer, int length, final boolean end) {
        return () -> new RawResult(String.format(Locale.getDefault(), "mock %d %s", index, end), index, end);
    }

    @Override
    public int packageSize() {
        return packageSize;
    }

    public void setPackageSize(int packageSize) {
        this.packageSize = packageSize;
    }

    @Override
    public int threadCount() {
        return threadCount;
    }

    @Override
    public boolean exhausted() {
        return false;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }
}
