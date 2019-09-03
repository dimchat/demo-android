package chat.dim.filesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Storage implements Writable {

    private byte[] fileContent = null;

    //---- read

    @Override
    public boolean exists(String filename) {
        File file = new File(filename);
        return file.exists() && file.length() > 0;
    }

    @Override
    public int load(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            // file not found
            throw new IOException("file not found: " + filename);
        }
        InputStream is = new FileInputStream(file);
        int size = is.available();
        fileContent = new byte[size];
        int len = is.read(fileContent, 0, size);
        is.close();
        if (len != size) {
            throw new IOException("reading error(" + len + " != " + size + "): " + filename);
        }
        return len;
    }

    @Override
    public byte[] getData() {
        return fileContent;
    }

    //---- write

    @Override
    public void setData(byte[] data) {
        fileContent = data;
    }

    @Override
    public int save(String filename) throws IOException {
        if (fileContent == null) {
            return -1;
        }
        File file = new File(filename);
        if (!file.exists()) {
            // check parent directory exists
            File dir = file.getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("failed to create directory: " + dir);
            }
        }
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(fileContent);
        fos.close();
        return fileContent.length;
    }

    @Override
    public boolean remove(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("file not found: " + filename);
        }
        return file.delete();
    }
}
