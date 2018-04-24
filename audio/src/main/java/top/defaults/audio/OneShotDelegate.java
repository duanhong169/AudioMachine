package top.defaults.audio;

public abstract class OneShotDelegate implements AudioProcessorDelegate {

    @Override
    public void initialize() {

    }

    @Override
    public void release() {

    }

    @Override
    public int packageSize() {
        return 3200; // 100ms in 16k sample rate
    }

    @Override
    public int threadCount() {
        return 1; // only support 1 thread, which "One Shot" means
    }
}
