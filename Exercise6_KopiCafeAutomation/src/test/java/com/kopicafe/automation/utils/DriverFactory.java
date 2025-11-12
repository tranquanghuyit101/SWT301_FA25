package com.kopicafe.automation.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class DriverFactory {

    public static WebDriver createDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--incognito");
        options.addArguments("--start-maximized");

        String headless = System.getProperty("headless", "false");
        if (Boolean.parseBoolean(headless)) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1280,720");
        }

        return new ChromeDriver(options);
    }
}
