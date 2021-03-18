package havis.app.modbus.reader.common;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NativeLibraryLoader {
    private final static Logger log = Logger.getLogger(NativeLibraryLoader.class.getName());

    private static Set<String> loaded = new HashSet<>();

    public void load(String nativeLibraryName) {
        if (!loaded.contains(nativeLibraryName)) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, "Loading native library " + nativeLibraryName);
            }
            System.loadLibrary(nativeLibraryName);
            loaded.add(nativeLibraryName);
        }
    }
}
