package com.kopicafe.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CartPage extends BasePage {

    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:3000")
                    .replaceAll("/$", "");

    // Nút Confirm and Pay
    private By confirmAndPayButton = By.xpath("//button[normalize-space()='Confirm and Pay']");

    // Radio chọn phương thức thanh toán (theo Cart/index.jsx)
    private By paymentCardRadio = By.id("paymentCard");
    private By paymentBankRadio = By.id("paymentBank");
    private By paymentCodRadio  = By.id("paymentCod");

    // 1 dòng item trong phần Order Summary: có text "x {qty}"
    private By cartItemQuantity = By.xpath(
            "//section[.//p[normalize-space()='Order Summary']]//p[starts-with(normalize-space(),'x ')]"
    );

    // Message giỏ hàng rỗng
    private By emptyCartMessage = By.xpath("//*[normalize-space()='Your cart is empty']");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    /** Mở trang giỏ hàng */
    public void open() {
        navigateTo(BASE_URL + "/cart");
    }

    public boolean isAtCartPage() {
        return driver.getCurrentUrl().contains("/cart");
    }

    public boolean isConfirmButtonEnabled() {
        return driver.findElement(confirmAndPayButton).isEnabled();
    }

    /** Có ít nhất 1 item trong giỏ hay không (dựa vào dòng 'x {qty}') */
    public boolean hasAtLeastOneCartItem() {
        return !driver.findElements(cartItemQuantity).isEmpty();
    }

    /** Giỏ hàng đang hiển thị trạng thái rỗng hay không */
    public boolean isCartEmptyMessageVisible() {
        return isElementVisible(emptyCartMessage);
    }

    /** Chọn phương thức thanh toán: Card */
    public void selectPaymentCard() {
        click(paymentCardRadio);
    }

    /** Chọn phương thức thanh toán: Bank transfer (nếu muốn dùng sau) */
    public void selectPaymentBank() {
        click(paymentBankRadio);
    }

    /** Chọn phương thức thanh toán: COD (nếu muốn dùng sau) */
    public void selectPaymentCod() {
        click(paymentCodRadio);
    }

    /** Chờ cho đến khi nút Confirm and Pay enable (đủ address + phone + payment + ship ok) */
    public void waitForConfirmButtonEnabled() {
        wait.until(driver -> {
            try {
                return driver.findElement(confirmAndPayButton).isEnabled();
            } catch (Exception e) {
                return false;
            }
        });
    }

    /** Click nút Confirm and Pay */
    public void clickConfirmAndPay() {
        click(confirmAndPayButton);
    }
}
