package huytq.example;

import java.util.logging.Logger;
import java.util.Objects;

public class NullPointerExample {

    private static final Logger logger = Logger.getLogger(NullPointerExample.class.getName());

    public static void main(String[] args) {
        String text = getTextFromInput();

        if (Objects.nonNull(text) && !text.isEmpty()) {
            logger.info("Text is not empty");
        } else if (text == null) {
            logger.warning("Text is null");
        } else {
            logger.warning("Text is empty");
        }
    }

    private static String getTextFromInput() {
        double random = Math.random();
        if (random < 0.33) return null;
        if (random < 0.66) return "";
        return "Hello";
    }
}
