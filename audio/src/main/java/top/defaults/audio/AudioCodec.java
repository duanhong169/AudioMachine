package top.defaults.audio;

public interface AudioCodec {

    int getFrameSize();

    int encode(short[] buffer, int length, byte[] encodedData);

    int decode(byte[] encodedData, int length, short[] buffer);

    void close();
}
