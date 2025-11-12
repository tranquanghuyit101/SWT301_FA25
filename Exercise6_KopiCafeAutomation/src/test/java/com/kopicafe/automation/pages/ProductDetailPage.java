package com.kopicafe.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ProductDetailPage extends BasePage {

    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:3000")
                    .replaceAll("/$", "");

    // ô nhập số lượng
    private By quantityInput   = By.name("custom-input-number");
    // nút Add to Cart trên ProductDetail
    private By addToCartButton = By.xpath("//button[normalize-space()='Add to Cart']");

    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }

    /** Mở đúng route chi tiết: /products/detail/{productId} */
    public void open(int productId) {
        navigateTo(BASE_URL + "/products/detail/" + productId);
    }

    public boolean isAtDetailPage(int productId) {
        return driver.getCurrentUrl().contains("/products/detail/" + productId);
    }

    public void setQuantity(int qty) {
        WebElement input = waitForVisibility(quantityInput);
        input.clear();
        input.sendKeys(String.valueOf(qty));
    }

    public void clickAddToCart() {
        click(addToCartButton);
    }
}
