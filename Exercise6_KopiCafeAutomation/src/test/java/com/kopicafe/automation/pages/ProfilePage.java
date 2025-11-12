package com.kopicafe.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ProfilePage extends BasePage {

    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:3000")
                    .replaceAll("/$", "");

    // Trong Profile/index.jsx có text "User Profile"
    private By heading    = By.xpath("//*[normalize-space()='User Profile']");
    // Ô email trong phần thông tin liên hệ
    private By emailInput = By.id("email");

    public ProfilePage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        navigateTo(BASE_URL + "/profile");
    }

    public boolean isProfileVisible() {
        return driver.getCurrentUrl().contains("/profile")
                && isElementVisible(heading)
                && isElementVisible(emailInput);
    }
}
