package com.kopicafe.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ForgotPassPage extends BasePage {

    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:3000")
                    .replaceAll("/$", "");

    private By emailField      = By.id("email");
    private By sendButton      = By.xpath("//button[normalize-space()='Send']");
    private By errorMessage    = By.xpath("//input[@id='email']/following-sibling::span");

    public ForgotPassPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        navigateTo(BASE_URL + "/auth/forgotpass");
    }

    public boolean isAtForgotPassPage() {
        return driver.getCurrentUrl().contains("/auth/forgotpass")
                && isElementVisible(emailField);
    }

    public void typeEmail(String email) {
        WebElement el = waitForVisibility(emailField);
        el.clear();
        el.sendKeys(email);
    }

    public void clickSend() {
        click(sendButton);
    }

    public String getErrorMessage() {
        try {
            return driver.findElement(errorMessage).getText();
        } catch (Exception e) {
            return "";
        }
    }
}
