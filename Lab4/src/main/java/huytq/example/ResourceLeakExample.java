package huytq.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResourceLeakExample {

    private static final Logger logger = Logger.getLogger(ResourceLeakExample.class.getName());

    public static void main(String[] args) {
        final String fileName = (args.length > 0) ? args[0] : "data.txt";
        final Path filePath = Paths.get(fileName).toAbsolutePath();

        if (!Files.exists(filePath)) {
            logger.warning(() -> String.format("File not found: %s", filePath));
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // ✅ Chỉ gọi log khi cấp độ INFO đang bật
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(String.format("Read line: %s", line));
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, String.format("Error reading file: %s", filePath), e);
        }
    }
}
