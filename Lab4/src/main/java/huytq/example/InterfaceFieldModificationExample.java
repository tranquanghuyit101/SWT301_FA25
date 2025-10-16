package huytq.example;

import java.util.logging.Logger;

// Class helper, không cần public vì nằm trong cùng file
final class AppConstants {
    private AppConstants() {
        // Ngăn tạo instance
    }

    public static final int MAX_USERS = 100;
}

public class InterfaceFieldModificationExample {
    private static final Logger logger = Logger.getLogger(InterfaceFieldModificationExample.class.getName());

    public static void main(String[] args) {
        logger.info(() -> String.format("Max users allowed: %d", AppConstants.MAX_USERS));
    }
}

