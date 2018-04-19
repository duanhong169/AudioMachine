package top.defaults.audio;

import java.util.concurrent.Callable;

public interface AudioProcessorDelegate {

    void initialize();

    void release();

    /**
     * Compose a {@link Callable} for {@link java.util.concurrent.ExecutorService#submit(Callable)},
     * return null to indicate that no callable is needed for the corresponding data.
     *
     * @param index index of the audio data package
     * @param buffer audio data buffer
     * @param length audio data length
     * @param end if this is the last audio package
     * @return A {@link Callable} to fetch audio process result.
     */
    Callable<RawResult> compose(int index, byte[] buffer, int length, boolean end);

    int packageSize();

    int threadCount();

    /**
     * Will not accept more data if it's exhausted. The host {@link AudioProcessor} should check
     * this flag and stop calling {@link AudioProcessorDelegate#compose(int, byte[], int, boolean)}
     * if true is returned.
     */
    boolean exhausted();
}
