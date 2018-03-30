package top.defaults.audio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class FileWriter {
    private String filePath;
    private FileOutputStream fos;

    FileWriter(String filePath) throws IOException {
        this.filePath = filePath;
        if (!new File(filePath).getParentFile().exists()) {
            new File(filePath).getParentFile().mkdirs();
        }
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

    int appendData(byte[] data, int offset, int length) {
        if (fos == null) return -1;
        try {
            fos.write(data, offset, length);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return length;
    }

    int appendData(byte[] data) {
        return appendData(data, 0, data.length);
    }
}
