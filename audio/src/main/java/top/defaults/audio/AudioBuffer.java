package top.defaults.audio;

import java.io.IOException;

public interface AudioBuffer {

    int readFully(byte[] sink) throws IOException;

    int read(byte[] sink, int offset, int byteCount);

    void write(byte[] source, int offset, int byteCount) throws IOException;

    /**
     * 标记buffer不再有新数据写入，标记后如果再调用{@link #write(byte[], int, int)}将抛出异常
     */
    void markAsFinished();

    boolean isFinished();
}
