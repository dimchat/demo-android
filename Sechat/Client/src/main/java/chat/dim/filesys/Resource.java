package chat.dim.filesys;

import java.io.IOException;
import java.io.InputStream;

public class Resource implements Readable {

    private byte[] fileContent = null;

    @Override
    public boolean exists(String filename) throws IOException {
        InputStream is = Resource.class.getResourceAsStream(filename);
        return is.available() > 0;
    }

    @Override
    public int load(String filename) throws IOException {
        InputStream is = Resource.class.getResourceAsStream(filename);
        int size = is.available();
        fileContent = new byte[size];
        int len = is.read(fileContent, 0, size);
        if (len != size) {
            throw new IOException("reading error(" + len + " != " + size + "): " + filename);
        }
        return len;
    }

    @Override
    public byte[] getData() {
        return fileContent;
    }
}
