package huytq.example;

import java.sql.*;
import java.util.logging.Logger;

public class SQLInjectionExample {
    private static final Logger logger = Logger.getLogger(SQLInjectionExample.class.getName());

    public static void main(String[] args) {
        String userInput = "' OR '1'='1";
        final String query = "SELECT username, email FROM users WHERE username = ?"; // chỉ chọn cột cần thiết

        // Đọc thông tin đăng nhập từ biến môi trường
        String dbUrl = "jdbc:mysql://localhost:3306/testdb";
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, userInput);
            logger.info(() -> String.format("Executing safe query with user input: %s", userInput));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    String email = rs.getString("email");
                    logger.info(() -> String.format("User found: %s, email: %s", username, email));
                }
            }
        } catch (SQLException e) {
            logger.severe(() -> String.format("Database error: %s", e.getMessage()));
        }
    }
}
