package top.defaults.audio;

import java.io.IOException;

public interface AudioBuffer {

    int readFully(byte[] sink) throws IOException;

    int read(byte[] sink, int offset, int byteCount);

    void write(byte[] source, int offset, int byteCount) throws IOException;

    /**
     * Mark this buffer as finished, which means this buffer won't accept for new data.
     * An {@link Error} will be thrown if {@link #write(byte[], int, int)} is called after
     * this method is called.
     */
    void markAsFinished();

    boolean isFinished();
}
