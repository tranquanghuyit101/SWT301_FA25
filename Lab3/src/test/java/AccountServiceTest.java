import huytq.example.AccountService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

public class AccountServiceTest {
    static AccountService accountService;

    @BeforeAll
    static void initAll(){
        accountService = new AccountService();
    }

    @AfterAll
    static void cleanupAll(){
        accountService = null;
    }

    @DisplayName("Kiểm tra tính hợp lệ của email")
    @ParameterizedTest
    @CsvFileSource(resources = "/email.csv", numLinesToSkip = 1)
    void testIsValidEmail(String email, Boolean expected){
        boolean result = accountService.isValidEmail(email);
        assertEquals(expected, result, () -> email + " should be an " + (expected? "valid" : "invalid") + " email");
    }

    @DisplayName("Kiểm tra chức năng đăng kí tài khoản")
    @ParameterizedTest
    @CsvFileSource(resources = "/test-data.csv", numLinesToSkip = 1)
    void testRegisterAccount(String username, String password, String email, Boolean expectedResult){
        if(expectedResult){
            assertTrue(accountService.registerAccount(username, password, email));
            return;
        }
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.registerAccount(username, password, email);
        });
    }
}
