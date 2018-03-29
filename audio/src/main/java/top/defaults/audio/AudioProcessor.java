package top.defaults.audio;

interface AudioProcessor {

    void initialize() throws Exception;

    void appendData(byte[] data, int offset, int length, boolean end);

    RawResult read() throws Error;

    void cancel();

    boolean isStopped();

    void release();

    String selfIntroduction();
}
