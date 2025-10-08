import huytq.example.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

public class AccountServiceTest {
    private final AccountService service = new AccountService();

    @ParameterizedTest(name = "Test {index}: username={0}, password={1}, email={2} → expected={3}")
    @CsvFileSource(resources = "/test-data.csv", numLinesToSkip = 1)
    void testRegisterAccount(String username, String password, String email, boolean expected) {
        boolean result = service.registerAccount(username, password, email);
        assertEquals(expected, result,
                () -> "❌ Failed for input: username=" + username + ", password=" + password + ", email=" + email);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/test-data.csv", numLinesToSkip = 1)
    void testValidEmail(String username, String password, String email, boolean expected) {
        boolean emailValid = service.isValidEmail(email);
        if (email.contains("@") && email.contains(".")) {
            assertTrue(emailValid, "Email should be valid: " + email);
        } else {
            assertFalse(emailValid, "Email should be invalid: " + email);
        }
    }






//    @Test
//    @DisplayName("Test duplicate username registration")
//    void testDuplicateUsername() {
//        // Lần đầu đăng ký - hợp lệ
//        boolean firstAttempt = service.registerAccount("john123", "Pass123!", "john@example.com");
//        assertTrue(firstAttempt, "First registration should succeed");
//
//        // Lần thứ hai cùng username - phải bị từ chối
//        boolean secondAttempt = service.registerAccount("john123", "Pass123!", "john@example.com");
//        assertFalse(secondAttempt, "Duplicate username should be rejected");
//    }
}