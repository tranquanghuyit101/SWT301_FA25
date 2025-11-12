package com.kopicafe.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class RegisterPage extends BasePage {

    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:3000")
                    .replaceAll("/$", "");

    // Các field trong form register
    private By emailField           = By.id("email");
    private By usernameField        = By.id("username");
    private By passwordField        = By.id("password");
    private By confirmPasswordField = By.id("confirmPassword");

    // Nút SUBMIT thực tế trên UI là "Signup", không phải "Register"
    private By signupButton = By.xpath("//button[normalize-space()='Signup']");

    // Error message dưới từng input
    private By emailErrorMessage    = By.xpath("//input[@id='email']/following-sibling::span");
    private By usernameErrorMessage = By.xpath("//input[@id='username']/following-sibling::span");
    private By passwordErrorMessage = By.xpath("//input[@id='password']/following-sibling::span");
    private By confirmErrorMessage  = By.xpath("//input[@id='confirmPassword']/following-sibling::span");

    public RegisterPage(WebDriver driver) {
        super(driver);
    }

    /** Mở trang /auth/register */
    public void open() {
        navigateTo(BASE_URL + "/auth/register");
    }

    /** Kiểm tra đang ở trang register (chấp nhận mọi biến thể /auth/register...) */
    public boolean isAtRegisterPage() {
        String url = driver.getCurrentUrl();
        return url.contains("/auth/register");
    }

    public void typeEmail(String email) {
        WebElement el = waitForVisibility(emailField);
        el.clear();
        el.sendKeys(email);
    }

    public void typeUsername(String username) {
        WebElement el = waitForVisibility(usernameField);
        el.clear();
        el.sendKeys(username);
    }

    public void typePassword(String password) {
        WebElement el = waitForVisibility(passwordField);
        el.clear();
        el.sendKeys(password);
    }

    public void typeConfirmPassword(String password) {
        WebElement el = waitForVisibility(confirmPasswordField);
        el.clear();
        el.sendKeys(password);
    }

    /** Click nút Signup (submit register form) */
    public void clickRegister() {
        click(signupButton);
    }

    /** Helper: điền full form */
    public void fillForm(String email, String username, String password, String confirm) {
        typeEmail(email);
        typeUsername(username);
        typePassword(password);
        typeConfirmPassword(confirm);
    }

    public String getEmailError() {
        return getTextSafe(emailErrorMessage);
    }

    public String getUsernameError() {
        return getTextSafe(usernameErrorMessage);
    }

    public String getPasswordError() {
        return getTextSafe(passwordErrorMessage);
    }

    public String getConfirmError() {
        return getTextSafe(confirmErrorMessage);
    }

    private String getTextSafe(By locator) {
        try {
            return driver.findElement(locator).getText();
        } catch (Exception e) {
            return "";
        }
    }
}
