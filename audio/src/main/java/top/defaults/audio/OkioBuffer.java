package top.defaults.audio;

import java.io.IOException;

import okio.Buffer;

public class OkioBuffer implements AudioBuffer {
    private Buffer buffer = new Buffer();
    private final Object bufferLock = new byte[0];
    private boolean isFinished;

    @Override
    public int readFully(byte[] sink) throws IOException {
        int offset = 0;
        while (offset < sink.length) {
            int read;
            synchronized (bufferLock) {
                read = read(sink, offset, sink.length - offset);
            }
            if (read <= 0) {
                if (isFinished) {
                    return offset;
                } else {
                    while (true) {
                        if (isFinished) return offset;
                        if (buffer.size() > 0) break;
                        if (Utils.napInterrupted()) {
                            throw new IOException(Utils.exceptionMessage(Error.ERROR_CLIENT, "Buffer interrupted."));
                        }
                    }
                }
            } else {
                offset += read;
            }
        }
        return sink.length;
    }

    @Override
    public int read(byte[] sink, int offset, int byteCount) {
        synchronized (bufferLock) {
            return buffer.read(sink, offset, byteCount);
        }
    }

    @Override
    public void write(byte[] source, int offset, int byteCount) throws IOException {
        if (isFinished) {
            throw new IOException(Utils.exceptionMessage(Error.ERROR_CLIENT,
                    "Buffer is marked as finished, write is prohibited. "));
        }
        synchronized (bufferLock) {
            buffer.write(source, offset, byteCount);
        }
    }

    @Override
    public void markAsFinished() {
        isFinished = true;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }
}
