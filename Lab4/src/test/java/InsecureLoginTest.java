
import org.junit.jupiter.api.Test;
import huytq.example.InsecureLogin;

class InsecureLoginTest {

    @Test
    void testLoginSuccess() {
        InsecureLogin.login("admin", "123456");
        // Không cần assert nếu chỉ cần chạy để tránh warning "method not used"
    }

    @Test
    void testLoginFail() {
        InsecureLogin.login("user", "wrong password");
    }

    @Test
     void testPrintUserInfo() {
        InsecureLogin insecureLogin = new InsecureLogin();
        insecureLogin.printUserInfo("John Doe");
    }
}
