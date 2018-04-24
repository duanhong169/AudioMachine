package top.defaults.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class RawCodec implements AudioCodec {

    @Override
    public int getFrameSize() {
        return 1280;
    }

    @Override
    public int encode(short[] buffer, int length, byte[] encodedData) {
        int lengthInBytes = length * 2;
        ByteBuffer byteBuffer = ByteBuffer.allocate(lengthInBytes).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : buffer) byteBuffer.putShort(s);
        System.arraycopy(byteBuffer.array(), 0, encodedData, 0, lengthInBytes);
        return lengthInBytes;
    }

    @Override
    public int decode(byte[] encodedData, int length, short[] buffer) {
        ByteBuffer.wrap(encodedData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buffer);
        return Math.min(length / 2, buffer.length);
    }

    @Override
    public void close() {

    }
}
