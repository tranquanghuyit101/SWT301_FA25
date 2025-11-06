package tests;

import org.junit.jupiter.api.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pages.LoginPage;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("KopiCoffee Login Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginTest extends BaseTest { // [cite: 185, 189]

    static LoginPage loginPage;
    static WebDriverWait wait;

    @BeforeAll
    static void initPage() {
        loginPage = new LoginPage(driver);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15)); // [cite: 192]
    }

    @Test
    @Order(1)
    @DisplayName("Should show error toast for wrong password")
    void testInvalidLogin() {
        loginPage.navigate();
        loginPage.login(
                "customer@example.com",
                "wrong-password",
                false
        );

        // Wait until error toast appears
        wait.until(d -> loginPage.isLoginErrorVisible());
        assertTrue(loginPage.isLoginErrorVisible(), "Expected error toast not visible");
    }

    @Test
    @Order(2)
    @DisplayName("Should login successfully with seeded customer account")
    void testSuccessfulLogin() {
        // Arrange / Act
        loginPage.navigate(); // [cite: 193]
        loginPage.login(
                "customer@example.com",
                "customer123",
                false
        );

        // Assert: URL should navigate to /products and token should be stored
        wait.until(ExpectedConditions.urlContains("/products"));
        String token = loginPage.getTokenFromLocalStorage();
        assertTrue(token != null && !token.isBlank(), "Token not found in localStorage after login");
    }


}