package havis.custom.harting.modbus.reader.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHandler {
    public InputStream newInputStream(Path file) throws IOException {
        Path jarContentPath = new PathHandler().getJARContentPath(file);
        if (jarContentPath != null) {
            return getClass().getClassLoader().getResourceAsStream(jarContentPath.toString());
        } else {
            try {
                return Files.newInputStream(file);
            } catch (IOException e) {
                throw new IOException("Cannot open file: " + file, e);
            }
        }
    }

    public String read(Path file, Charset encoding) throws IOException {
        byte[] encoded = null;
        Path jarContentPath = new PathHandler().getJARContentPath(file);
        if (jarContentPath != null) {
            try {
                InputStream is = getClass().getClassLoader()
                                .getResourceAsStream(jarContentPath.toString());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int c = 0;
                while ((c = is.read()) != -1) {
                    bos.write((char) c);
                }
                encoded = bos.toByteArray();
            } catch (IOException e) {
                throw new IOException("Cannot read file from JAR: " + jarContentPath, e);
            }
        } else {
            try {
                encoded = Files.readAllBytes(file);
            } catch (IOException e) {
                throw new IOException("Cannot read file: " + file, e);
            }
        }
        return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }
}
