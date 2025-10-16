package huytq.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PathTraversalExample {

    private static final Logger logger = Logger.getLogger(PathTraversalExample.class.getName());
    private static final Path BASE_DIR = Paths.get("data").toAbsolutePath().normalize();

    public static void main(String[] args) {
        if (args.length == 0) {
            logger.warning("No filename provided. Usage: java PathTraversalExample <filename>");
            return;
        }

        String userInput = args[0];
        try {
            readFileSafely(userInput);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO error while reading file", e);
        } catch (SecurityException | InvalidPathException _) {
            logger.log(Level.WARNING, "Invalid or forbidden path: {0}", userInput);
        }
    }

    public static void readFileSafely(String requested) throws IOException {
        if (requested == null || requested.isBlank()) {
            throw new InvalidPathException(String.valueOf(requested), "Requested filename is empty");
        }

        Path requestedPath = Paths.get(requested);
        Path resolved = BASE_DIR.resolve(requestedPath).toAbsolutePath().normalize();

        if (!resolved.startsWith(BASE_DIR)) {
            throw new SecurityException("Attempted path traversal or access outside allowed directory");
        }

        if (!resolved.getFileName().toString().matches("[a-zA-Z0-9_\\-]+\\.txt")) {
            throw new SecurityException("Filename not allowed by policy");
        }

        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            logger.log(Level.INFO, "Requested file not found or not a regular file: {0}", resolved.getFileName());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(resolved)) {
            String firstLine = reader.readLine();
            if (firstLine != null) {
                logger.log(Level.INFO, "Successfully read first line of {0}: {1}",
                        new Object[]{resolved.getFileName(), firstLine});
            } else {
                logger.log(Level.INFO, "File is empty: {0}", resolved.getFileName());
            }
        }
    }
}
