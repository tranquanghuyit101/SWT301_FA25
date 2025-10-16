package huytq.example;

import java.util.logging.Logger;

public class HardcodedCredentialsExample {

    private static final Logger logger = Logger.getLogger(HardcodedCredentialsExample.class.getName());

    public static void main(String[] args) {
        String username = "admin";
        String password = System.getenv("ADMIN_PASSWORD"); // đọc mật khẩu từ biến môi trường

        if (authenticate(username, password)) {
            logger.info("Access granted");
        } else {
            logger.warning("Access denied");
        }
    }

    private static boolean authenticate(String user, String pass) {
        String correctPassword = System.getenv("ADMIN_PASSWORD"); // đọc từ biến môi trường
        return "admin".equals(user) && correctPassword != null && correctPassword.equals(pass);
    }
}
