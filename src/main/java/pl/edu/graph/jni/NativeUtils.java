package pl.edu.graph.jni;

import java.io.*;

/**
 * Utility class for loading native libraries from within JAR files.
 */
public class NativeUtils {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("windows");
    private static final boolean IS_LINUX = OS_NAME.contains("linux");
    
    /**
     * Loads a native library using appropriate platform-specific library name.
     */
    public static void loadLibraryFromJar(String path) throws IOException {
        String fullPath;
        
        if (IS_WINDOWS) {
            fullPath = path + ".dll";
        } else if (IS_LINUX) {
            fullPath = path + ".so";
        } else {
            throw new UnsupportedOperationException("Unsupported platform: " + OS_NAME);
        }
        
        // Get the resource as a stream
        try (InputStream inputStream = NativeUtils.class.getResourceAsStream(fullPath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Native library not found: " + fullPath);
            }
            
            // Create a temporary file
            File tempFile = File.createTempFile("lib", 
                IS_WINDOWS ? ".dll" : ".so");
            tempFile.deleteOnExit();
            
            // Copy the library to the temporary file
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            // Load the library from the temporary file
            System.load(tempFile.getAbsolutePath());
        }
    }
}