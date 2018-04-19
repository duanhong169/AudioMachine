package top.defaults.audio;

interface AudioProcessor {

    void initialize() throws Exception;

    void appendData(byte[] data, int offset, int length, boolean end);

    RawResult read() throws Error;

    void cancel();

    boolean isStopped();

    void release();

    /**
     * Will not accept more data if it's exhausted. The host {@link AudioMachine} should check
     * this flag and stop calling {@link AudioProcessor#appendData(byte[], int, int, boolean)}
     * if true is returned.
     */
    boolean exhausted();

    String selfIntroduction();
}
