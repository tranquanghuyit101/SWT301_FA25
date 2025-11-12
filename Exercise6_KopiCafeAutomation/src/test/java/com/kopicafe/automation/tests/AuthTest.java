package com.kopicafe.automation.tests;

import com.kopicafe.automation.pages.ForgotPassPage;
import com.kopicafe.automation.pages.LoginPage;
import com.kopicafe.automation.pages.ProfilePage;
import com.kopicafe.automation.pages.RegisterPage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.JavascriptExecutor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Kopi Cafe Authentication System Tests (Login + Profile + Register + Forgot Password)")
public class AuthTest extends BaseTest {

    static LoginPage loginPage;
    static RegisterPage registerPage;
    static ProfilePage profilePage;
    static ForgotPassPage forgotPassPage;

    // base URL cho FE
    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:3000")
                    .replaceAll("/$", "");

    @BeforeAll
    static void initPages() {
        loginPage      = new LoginPage(driver);
        registerPage   = new RegisterPage(driver);
        profilePage    = new ProfilePage(driver);
        forgotPassPage = new ForgotPassPage(driver);
    }

    /** Đưa browser về trạng thái guest "sạch" */
    private void resetAuthState() {
        driver.manage().deleteAllCookies();
        driver.get(BASE_URL + "/");
        ((JavascriptExecutor) driver)
                .executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
    }

    // =========================
    // FEATURE: LOGIN
    // =========================

    @ParameterizedTest
    @Order(1)
    @DisplayName("LOGIN_001 - Sai email/password → vẫn ở trang login")
    @CsvSource({
            "admin@example.com, wrongpass",
            "invalid@example.com, admin123"
    })
    void loginInvalidCredentials(String email, String password) {
        resetAuthState();

        loginPage.open();
        loginPage.login(email, password);

        // Backend trả lỗi, FE giữ user ở /auth/login
        loginPage.waitForUrlContains("/auth/login");
        assertTrue(
                loginPage.isAtLoginPage(),
                "Khi login sai, user vẫn phải ở /auth/login"
        );
    }

    @Test
    @Order(2)
    @DisplayName("LOGIN_002 - Login thành công với tài khoản hợp lệ")
    void loginSuccess() {
        resetAuthState();

        loginPage.open();
        loginPage.login("admin@example.com", "admin123");

        loginPage.waitForUrlContains("/products");
        assertTrue(
                driver.getCurrentUrl().contains("/products"),
                "After successful login, user should be redirected to /products"
        );
    }

    @Test
    @Order(3)
    @DisplayName("Extra Login - Không nhập email/password → báo lỗi required")
    void loginRequiredFieldsValidation() {
        resetAuthState();

        loginPage.open();
        assertTrue(loginPage.isAtLoginPage(), "Phải đang ở /auth/login");

        // Submit form trống
        loginPage.login("", "");

        String emailError    = loginPage.getEmailErrorMessage();
        String passwordError = loginPage.getPasswordErrorMessage();

        assertFalse(emailError.isBlank(),    "Email error phải hiển thị khi để trống");
        assertFalse(passwordError.isBlank(), "Password error phải hiển thị khi để trống");
    }

    // =========================
    // FEATURE: PROFILE
    // =========================

    @Test
    @Order(4)
    @DisplayName("PROFILE_001 - Guest truy cập /profile bị redirect về /auth/login")
    void profileRequiresAuthentication() {
        resetAuthState();

        // Mở thẳng /profile
        profilePage.open();
        profilePage.waitForUrlContains("/auth/login");

        assertTrue(
                loginPage.isAtLoginPage(),
                "Guest truy cập /profile phải bị chuyển về trang login"
        );
    }

    @Test
    @Order(5)
    @DisplayName("PROFILE_002 - User đã login có thể xem trang Profile")
    void profileVisibleForLoggedInUser() {
        resetAuthState();

        // Login trước
        loginPage.open();
        loginPage.login("admin@example.com", "admin123");
        loginPage.waitForUrlContains("/products");

        // Sau đó mở profile
        profilePage.open();
        profilePage.waitForUrlContains("/profile");

        assertTrue(
                profilePage.isProfileVisible(),
                "Profile page phải hiển thị cho user đã đăng nhập"
        );
    }

    // =========================
    // FEATURE: REGISTER
    // =========================

    @Test
    @Order(6)
    @DisplayName("REGISTER_001 - Bỏ trống field register → hiển thị lỗi required")
    void registerRequiredFieldsValidation() {
        resetAuthState();

        registerPage.open();
        assertTrue(
                registerPage.isAtRegisterPage(),
                "Form register phải nằm trong flow /auth/register"
        );

        registerPage.fillForm("", "", "", "");
        registerPage.clickRegister();

        String emailErr    = registerPage.getEmailError();
        String usernameErr = registerPage.getUsernameError();
        String passErr     = registerPage.getPasswordError();
        String confirmErr  = registerPage.getConfirmError();

        assertFalse(emailErr.isBlank(),    "Email error phải hiển thị khi để trống");
        assertFalse(usernameErr.isBlank(), "Username error phải hiển thị khi để trống");
        assertFalse(passErr.isBlank(),     "Password error phải hiển thị khi để trống");
        assertFalse(confirmErr.isBlank(),  "Confirm password error phải hiển thị khi để trống");
    }

    @Test
    @Order(7)
    @DisplayName("REGISTER_002 - Password & Confirm không khớp → báo lỗi")
    void registerPasswordConfirmMismatch() {
        resetAuthState();

        registerPage.open();
        assertTrue(registerPage.isAtRegisterPage(), "Phải đang ở trang register");

        String randomEmail = "test+" + UUID.randomUUID() + "@example.com";
        String username    = "testuser_" + UUID.randomUUID().toString().substring(0, 6);

        registerPage.fillForm(
                randomEmail,
                username,
                "Password123!",
                "Different999!"
        );
        registerPage.clickRegister();

        String confirmErr = registerPage.getConfirmError();
        assertFalse(
                confirmErr.isBlank(),
                "Khi password & confirm không khớp, phải có message lỗi ở confirm password"
        );
    }

    @Test
    @Order(8)
    @DisplayName("REGISTER_003 - Đăng ký với dữ liệu hợp lệ")
    void registerSuccessWithValidData() {
        resetAuthState();

        registerPage.open();
        assertTrue(registerPage.isAtRegisterPage(), "Phải đang ở trang register");

        String randomEmail = "student+" + UUID.randomUUID() + "@example.com";
        String username    = "autoUser_" + UUID.randomUUID().toString().substring(0, 6);
        String password    = "Password123!";

        registerPage.fillForm(
                randomEmail,
                username,
                password,
                password
        );
        registerPage.clickRegister();

        String emailErr    = registerPage.getEmailError();
        String usernameErr = registerPage.getUsernameError();
        String passErr     = registerPage.getPasswordError();
        String confirmErr  = registerPage.getConfirmError();

        assertTrue(emailErr.isBlank(),    "Sau khi register hợp lệ, không được còn lỗi email");
        assertTrue(usernameErr.isBlank(), "Sau khi register hợp lệ, không được còn lỗi username");
        assertTrue(passErr.isBlank(),     "Sau khi register hợp lệ, không được còn lỗi password");
        assertTrue(confirmErr.isBlank(),  "Sau khi register hợp lệ, không được còn lỗi confirm");

        String currentUrl = driver.getCurrentUrl();
        assertTrue(
                currentUrl.contains("/auth"),
                "Sau bước register, hệ thống vẫn nằm trong flow auth (login/verify), URL: " + currentUrl
        );
    }

    // =========================
    // FEATURE: FORGOT PASSWORD
    // =========================

    @Test
    @Order(9)
    @DisplayName("FORGOT_001 - Từ login bấm 'Forgot password?' → chuyển sang /auth/forgotpass")
    void navigateToForgotPasswordFromLogin() {
        resetAuthState();

        loginPage.open();
        assertTrue(loginPage.isAtLoginPage(), "Phải đang ở trang login");

        loginPage.clickForgotPasswordLink();
        loginPage.waitForUrlContains("/auth/forgotpass");

        assertTrue(
                forgotPassPage.isAtForgotPassPage(),
                "Click 'Forgot password?' phải chuyển sang trang /auth/forgotpass"
        );
    }

    @Test
    @Order(10)
    @DisplayName("FORGOT_002 - Quên mật khẩu - bỏ trống email → hiển thị 'Must input email!'")
    void forgotPasswordEmailRequired() {
        resetAuthState();

        forgotPassPage.open();
        assertTrue(
                forgotPassPage.isAtForgotPassPage(),
                "Phải đang ở trang /auth/forgotpass"
        );

        // Đảm bảo email trống rồi bấm Send
        forgotPassPage.typeEmail("");
        forgotPassPage.clickSend();

        String error = forgotPassPage.getErrorMessage();
        assertEquals(
                "Must input email!",
                error,
                "Khi bỏ trống email ở Forgot Password, phải hiển thị 'Must input email!'"
        );
    }
}
