package top.defaults.audio;

import java.io.FileOutputStream;
import java.io.IOException;

class FileWriter {
    private String filePath;
    private FileOutputStream fos = null;

    FileWriter(String filePath) throws IOException {
        this.filePath = filePath;
        fos = new FileOutputStream(filePath, true);
    }

    void clear() {
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void appendData(byte[] data, int offset, int length) {
        if (fos == null) return;
        try {
            fos.write(data, offset, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void appendData(byte[] data) {
        appendData(data, 0, data.length);
    }
}
