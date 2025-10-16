package huytq.example;

import java.util.logging.Logger;

interface LoginHandler {
    void login(String username, String password);
}

public class InterfaceNamingInconsistencyExample {
    private static final Logger logger = Logger.getLogger(InterfaceNamingInconsistencyExample.class.getName());

    private static class BasicLoginHandler implements LoginHandler {
        @Override
        public void login(String username, String password) {
            logger.info(() -> String.format("User %s logged in successfully", username));
        }
    }

    public static void main(String[] args) {
        LoginHandler handler = new BasicLoginHandler();
        handler.login("admin", "123456");
    }
}
