package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    // 1. Constructor
    public LoginPage(WebDriver driver) {
        super(driver); // [cite: 163]
    }

    // 2. Locators for KopiCoffee Login page
    private By emailField = By.id("email");
    private By passwordField = By.id("password");
    private By rememberCheckbox = By.id("remember");
    private By loginButton = By.xpath("//button[@type='submit' and normalize-space()='Login']");
    private By errorToast = By.xpath("//*[contains(.,'Incorrect email or password') or contains(.,'Sai tài khoản hoặc mật khẩu')]");

    // 3. Actions

    /**
     * Điều hướng đến trang đăng nhập của KopiCoffee FE
     */
    public void navigate() {
        navigateTo("http://localhost:3000/auth/login");
    }

    /**
     * Điền email, password và click Login
     */
    public void login(String email, String password, boolean rememberMe) {
        type(emailField, email);
        type(passwordField, password);
        if (rememberMe) {
            jsClick(rememberCheckbox);
        }
        jsClick(loginButton);
    }

    /**
     * Kiểm tra thông báo lỗi đăng nhập hiển thị
     */
    public boolean isLoginErrorVisible() {
        try {
            java.util.List<org.openqa.selenium.WebElement> list = driver.findElements(errorToast);
            for (org.openqa.selenium.WebElement el : list) {
                if (el.isDisplayed()) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public By getErrorToastLocator() {
        return errorToast;
    }

    /**
     * Lấy JWT token đã lưu trong localStorage (kopi_token)
     */
    public String getTokenFromLocalStorage() {
        try {
            Object result = ((JavascriptExecutor) driver)
                    .executeScript("return window.localStorage.getItem('kopi_token');");
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}