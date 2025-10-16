package huytq.example;

import java.util.logging.Logger;

public class CatchGenericExceptionExample {

    private static final Logger logger = Logger.getLogger(CatchGenericExceptionExample.class.getName());

    public static void main(String[] args) {
        try {
            // Giả lập dữ liệu có thể null
            String s = getStringFromUserInput();

            if (s != null) {
                logger.info(() -> "Length: " + s.length());
            } else {
                logger.warning("String 's' is null, skipping length check.");
            }
        } catch (Exception _) { // Dùng unnamed pattern thay cho 'e'
            logger.severe("An unexpected error occurred.");
        }
    }

    // Giả lập hàm trả về chuỗi có thể null
    private static String getStringFromUserInput() {
        return Math.random() > 0.5 ? "Hello" : null;
    }
}
