package top.defaults.audio;

import java.io.IOException;

public interface AudioSource {

    int getSampleRate();

    void open() throws IOException;

    int readFully(byte[] buffer) throws IOException;

    int read(byte[] buffer) throws IOException;

    int read(byte[] buffer, int byteOffset, int byteCount) throws IOException;

    void close() throws IOException;
}
