package com.kopicafe.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    // Cho phép override bằng -DbaseUrl=..., mặc định localhost:3000
    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:3000")
                    .replaceAll("/$", "");

    private By emailField    = By.id("email");
    private By passwordField = By.id("password");
    private By loginButton   = By.xpath("//button[normalize-space()='Login']");

    // Link "Forgot password?" (dùng xpath linh hoạt theo text, không bị lệ thuộc hoa/thường, có/không có ?)
    private By forgotPasswordLink = By.xpath(
            "//a[contains(translate(normalize-space(.)," +
                    "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                    "'abcdefghijklmnopqrstuvwxyz')," +
                    "'forgot password')]"
    );

    // Span hiển thị lỗi validate dưới ô email & password
    private By emailErrorMessage    = By.xpath("//input[@id='email']/following-sibling::span");
    private By passwordErrorMessage = By.xpath("//input[@id='password']/following-sibling::span");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /** Mở trang login */
    public void open() {
        navigateTo(BASE_URL + "/auth/login");
    }

    /** Lấy nội dung lỗi validate của ô email */
    public String getEmailErrorMessage() {
        try {
            return driver.findElement(emailErrorMessage).getText();
        } catch (Exception e) {
            return "";
        }
    }

    /** Lấy nội dung lỗi validate của ô password */
    public String getPasswordErrorMessage() {
        try {
            return driver.findElement(passwordErrorMessage).getText();
        } catch (Exception e) {
            return "";
        }
    }

    /** Thực hiện login với email & password */
    public void login(String email, String password) {
        type(emailField, email);
        type(passwordField, password);
        click(loginButton);
    }

    /** Click link "Forgot password?" để chuyển sang màn quên mật khẩu */
    public void clickForgotPasswordLink() {
        click(forgotPasswordLink);
    }

    /** Đang đứng đúng trang login hay không */
    public boolean isAtLoginPage() {
        return driver.getCurrentUrl().contains("/auth/login")
                && isElementVisible(emailField)
                && isElementVisible(passwordField);
    }
}
