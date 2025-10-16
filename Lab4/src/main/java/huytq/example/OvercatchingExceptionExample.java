package huytq.example;

import java.util.logging.Logger;

public class OvercatchingExceptionExample {

    private static final Logger logger = Logger.getLogger(OvercatchingExceptionExample.class.getName());

    public static void main(String[] args) {
        try {
            int[] arr = new int[5];
            logger.info(() -> "Accessing element: " + arr[10]); // Sẽ gây lỗi ArrayIndexOutOfBoundsException
        } catch (RuntimeException _) { // Dùng unnamed pattern thay cho e
            logger.severe("Runtime error occurred while accessing array element.");
        }
    }
}
