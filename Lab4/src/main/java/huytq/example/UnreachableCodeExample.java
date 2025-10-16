package huytq.example;

import java.util.logging.Logger;

public class UnreachableCodeExample {
    private static final Logger logger = Logger.getLogger(UnreachableCodeExample.class.getName());

    // Thay vì method, dùng hằng số
    private static final int NUMBER = 42;

    public static void main(String[] args) {
        logger.info(() -> String.format("Number: %d", NUMBER));
    }
}
