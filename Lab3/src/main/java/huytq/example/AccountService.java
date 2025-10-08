package huytq.example;

import java.util.HashSet;
import java.util.Set;

public class AccountService {

    private static final Set<String> existingAccounts = new HashSet<>();

    public boolean registerAccount(String username, String password, String email) {
        if (username == null || username.isEmpty() || username.length() <= 3) {
            return false;
        }

        if (existingAccounts.contains(username)) {
            // tài khoản đã tồn tại
            return false;
        }

        if (password == null || !isValidPassword(password)) {
            return false;
        }

        if (!isValidEmail(email)) {
            return false;
        }

        // đăng ký thành công → lưu tài khoản
        existingAccounts.add(username);
        return true;
    }

    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPassword(String password) {
        // ít nhất 6 ký tự, có chữ hoa, số, ký tự đặc biệt
        return password.length() > 6
                && password.matches(".*[A-Z].*")
                && password.matches(".*[0-9].*")
                && password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }
}
