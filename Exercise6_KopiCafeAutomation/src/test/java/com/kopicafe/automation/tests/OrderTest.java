package com.kopicafe.automation.tests;

import com.kopicafe.automation.pages.CartPage;
import com.kopicafe.automation.pages.LoginPage;
import com.kopicafe.automation.pages.ProductDetailPage;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Kopi Cafe Order / Cart / History System Tests")
public class OrderTest extends BaseTest {

    private static LoginPage loginPage;
    private static CartPage cartPage;
    private static ProductDetailPage productDetailPage;

    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:3000")
                    .replaceAll("/$", "");

    @BeforeAll
    static void setUpPages() {
        loginPage         = new LoginPage(driver);
        cartPage          = new CartPage(driver);
        productDetailPage = new ProductDetailPage(driver);
    }

    /** Reset cookie + local/session storage để tránh token/cart cũ giữa các test */
    private void resetState() {
        driver.manage().deleteAllCookies();
        driver.get(BASE_URL + "/");
        ((JavascriptExecutor) driver)
                .executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
    }

    // ========================================================
    // ORDER-01: Guard /cart cho guest
    // ========================================================

    @Test
    @Order(1)
    @DisplayName("ORDER-01: Guest truy cập /cart bị redirect về /auth/login")
    void guestCannotAccessCartWithoutLogin() {
        resetState();

        cartPage.open();
        cartPage.waitForUrlContains("/auth/login");

        assertTrue(
                loginPage.isAtLoginPage(),
                "Guest truy cập /cart phải bị chuyển về /auth/login"
        );
    }

    // ========================================================
    // ORDER-02: User login truy cập /cart (empty)
    // ========================================================

    @Test
    @Order(2)
    @DisplayName("ORDER-02: User đã login có thể mở /cart (giỏ hàng ban đầu rỗng)")
    void loggedInUserCanAccessEmptyCart() {
        resetState();

        // Login
        loginPage.open();
        loginPage.login("admin@example.com", "admin123");
        loginPage.waitForUrlContains("/products");

        // Mở cart
        cartPage.open();
        cartPage.waitForUrlContains("/cart");

        assertTrue(cartPage.isAtCartPage(), "User đã login phải truy cập được /cart");
        // Giỏ hàng mới → thường chưa có item
        assertTrue(
                cartPage.isCartEmptyMessageVisible() || !cartPage.hasAtLeastOneCartItem(),
                "Giỏ hàng mới login thường rỗng (hiển thị 'Your cart is empty' hoặc không có dòng Order Summary)."
        );
    }

    // ========================================================
    // ORDER-03: Add 1 sản phẩm từ Product Detail vào cart
    // ========================================================

    @Test
    @Order(3)
    @DisplayName("ORDER-03: User login có thể thêm sản phẩm từ trang detail vào giỏ hàng")
    void loggedInUserCanAddProductToCart() {
        resetState();

        // Login
        loginPage.open();
        loginPage.login("admin@example.com", "admin123");
        loginPage.waitForUrlContains("/products");

        // Mở chi tiết sản phẩm id=1
        int productId = 1;
        productDetailPage.open(productId);
        productDetailPage.waitForUrlContains("/products/detail/" + productId);
        assertTrue(
                productDetailPage.isAtDetailPage(productId),
                "Phải đang ở /products/detail/" + productId
        );

        // Nhập số lượng hợp lệ & Add to Cart
        productDetailPage.setQuantity(1);
        productDetailPage.clickAddToCart();

        // Mở giỏ hàng kiểm tra Order Summary
        cartPage.open();
        cartPage.waitForUrlContains("/cart");

        assertTrue(
                cartPage.hasAtLeastOneCartItem(),
                "Sau khi Add to Cart, Order Summary phải có ít nhất 1 item"
        );
    }

    // ========================================================
    // ORDER-04: Cart giữ item khi điều hướng giữa các trang
    // ========================================================

    @Test
    @Order(4)
    @DisplayName("ORDER-04: Item trong giỏ vẫn còn khi điều hướng sang trang khác rồi quay lại /cart")
    void itemsRemainInCartAfterNavigation() {
        resetState();

        // Login
        loginPage.open();
        loginPage.login("admin@example.com", "admin123");
        loginPage.waitForUrlContains("/products");

        // Thêm 1 sản phẩm vào cart
        int productId = 1;
        productDetailPage.open(productId);
        productDetailPage.waitForUrlContains("/products/detail/" + productId);
        productDetailPage.setQuantity(1);
        productDetailPage.clickAddToCart();

        // Lần 1 mở cart
        cartPage.open();
        cartPage.waitForUrlContains("/cart");
        assertTrue(
                cartPage.hasAtLeastOneCartItem(),
                "Sau khi Add to Cart, giỏ hàng phải có item"
        );

        // Điều hướng sang /products rồi quay lại /cart
        driver.get(BASE_URL + "/products");
        loginPage.waitForUrlContains("/products");

        cartPage.open();
        cartPage.waitForUrlContains("/cart");

        assertTrue(
                cartPage.hasAtLeastOneCartItem(),
                "Sau khi điều hướng sang trang khác rồi quay lại /cart, item trong giỏ vẫn phải được giữ lại"
        );
    }

    // ========================================================
    // ORDER-05: Confirm and Pay disabled khi thiếu thông tin
    // ========================================================

    @Test
    @Order(5)
    @DisplayName("ORDER-05: Đã login nhưng chưa cung cấp thông tin checkout → 'Confirm and Pay' phải disabled")
    void confirmButtonDisabledWhenNoPaymentSelected() {
        resetState();

        // Login
        loginPage.open();
        loginPage.login("admin@example.com", "admin123");
        loginPage.waitForUrlContains("/products");

        // Mở cart (chưa chọn payment / shipping info)
        cartPage.open();
        cartPage.waitForUrlContains("/cart");

        assertFalse(
                cartPage.isConfirmButtonEnabled(),
                "Khi chưa chọn payment/address/phone hợp lệ, nút Confirm and Pay phải disabled"
        );
    }

    // ========================================================
    // ORDER-06: Guest không được truy cập /history
    // ========================================================

    @Test
    @Order(6)
    @DisplayName("ORDER-06: Guest truy cập /history bị redirect về /auth/login")
    void guestCannotAccessHistoryWithoutLogin() {
        resetState();

        driver.get(BASE_URL + "/history");
        cartPage.waitForUrlContains("/auth/login");

        assertTrue(
                loginPage.isAtLoginPage(),
                "Guest truy cập /history phải bị redirect về /auth/login"
        );
    }

    // ========================================================
    // ORDER-07: User login xem được /history
    // ========================================================

    @Test
    @Order(7)
    @DisplayName("ORDER-07: User login có thể xem trang lịch sử đơn hàng (/history)")
    void loggedInUserCanAccessHistory() {
        resetState();

        // Login
        loginPage.open();
        loginPage.login("admin@example.com", "admin123");
        loginPage.waitForUrlContains("/products");

        // Mở history
        driver.get(BASE_URL + "/history");
        cartPage.waitForUrlContains("/history");

        assertTrue(
                driver.getCurrentUrl().contains("/history"),
                "User đã login phải truy cập được trang /history mà không bị redirect về /auth/login"
        );
    }

    // ========================================================
    // ORDER-08: Sau khi thanh toán thành công, giỏ hàng được clear
    // ========================================================

    @Test
    @Order(8)
    @DisplayName("ORDER-08: Sau khi Confirm and Pay thành công, mở lại /cart thì giỏ hàng phải rỗng")
    void cartIsEmptyAfterSuccessfulPayment() {
        resetState();

        // 1. Login
        loginPage.open();
        loginPage.login("admin@example.com", "admin123");
        loginPage.waitForUrlContains("/products");

        // 2. Thêm 1 sản phẩm vào giỏ
        int productId = 1;
        productDetailPage.open(productId);
        productDetailPage.waitForUrlContains("/products/detail/" + productId);
        productDetailPage.setQuantity(1);
        productDetailPage.clickAddToCart();

        // 3. Mở /cart và kiểm tra có item
        cartPage.open();
        cartPage.waitForUrlContains("/cart");
        assertTrue(
                cartPage.hasAtLeastOneCartItem(),
                "Trước khi thanh toán, giỏ phải có ít nhất 1 item"
        );

        // 4. Chọn payment (Card) → chờ nút Confirm and Pay enable → click thanh toán
        cartPage.selectPaymentCard();
        cartPage.waitForConfirmButtonEnabled();
        assertTrue(
                cartPage.isConfirmButtonEnabled(),
                "Sau khi chọn payment và đủ thông tin, nút Confirm and Pay phải enable"
        );

        cartPage.clickConfirmAndPay();

        // 5. Sau thanh toán, FE điều hướng sang /history
        cartPage.waitForUrlContains("/history");
        assertTrue(
                driver.getCurrentUrl().contains("/history"),
                "Sau khi Confirm and Pay thành công, hệ thống phải chuyển sang /history"
        );

        // 6. Mở lại /cart → giỏ phải rỗng
        cartPage.open();
        cartPage.waitForUrlContains("/cart");

        assertTrue(
                cartPage.isCartEmptyMessageVisible() || !cartPage.hasAtLeastOneCartItem(),
                "Sau khi thanh toán xong, mở lại /cart thì giỏ hàng phải rỗng (không còn item trong Order Summary)"
        );
    }

    // ========================================================
    // ORDER-09: Refresh /cart sau khi add → item vẫn còn
    // ========================================================

    @Test
    @Order(9)
    @DisplayName("ORDER-09: Refresh /cart sau khi thêm sản phẩm → item vẫn còn trong giỏ")
    void cartKeepsItemsAfterRefresh() {
        resetState();

        // Login
        loginPage.open();
        loginPage.login("admin@example.com", "admin123");
        loginPage.waitForUrlContains("/products");

        // Thêm sản phẩm vào cart
        int productId = 1;
        productDetailPage.open(productId);
        productDetailPage.waitForUrlContains("/products/detail/" + productId);
        productDetailPage.setQuantity(1);
        productDetailPage.clickAddToCart();

        // Mở /cart
        cartPage.open();
        cartPage.waitForUrlContains("/cart");
        assertTrue(cartPage.hasAtLeastOneCartItem());

        // Refresh
        driver.navigate().refresh();
        cartPage.waitForUrlContains("/cart");

        assertTrue(
                cartPage.hasAtLeastOneCartItem(),
                "Sau khi refresh /cart, item vừa thêm vẫn phải còn trong giỏ"
        );
    }

    // ========================================================
    // ORDER-10: E2E: Login → Add to cart → chọn payment → Confirm and Pay → /history
    // ========================================================

    @Test
    @Order(10)
    @DisplayName("ORDER-10: E2E: Login → thêm sản phẩm → chọn payment → Confirm and Pay → chuyển sang /history")
    void endToEndLoginAddToCartThenPayAndGoToHistory() {
        resetState();

        // 1. Login
        loginPage.open();
        loginPage.login("admin@example.com", "admin123");
        loginPage.waitForUrlContains("/products");

        // 2. Thêm sản phẩm vào giỏ
        int productId = 1;
        productDetailPage.open(productId);
        productDetailPage.waitForUrlContains("/products/detail/" + productId);
        productDetailPage.setQuantity(1);
        productDetailPage.clickAddToCart();

        // 3. Mở cart, kiểm tra có item
        cartPage.open();
        cartPage.waitForUrlContains("/cart");
        assertTrue(
                cartPage.hasAtLeastOneCartItem(),
                "Trước khi thanh toán, giỏ phải có ít nhất 1 item"
        );

        // 4. Chọn phương thức thanh toán (Card) và chờ nút Confirm and Pay enable
        cartPage.selectPaymentCard();
        cartPage.waitForConfirmButtonEnabled();
        assertTrue(
                cartPage.isConfirmButtonEnabled(),
                "Sau khi chọn payment, cung cấp đủ địa chỉ & số điện thoại, nút Confirm and Pay phải enable"
        );

        // 5. Click Confirm and Pay → FE gọi payHandler, tạo transaction và navigate('/history')
        cartPage.clickConfirmAndPay();

        // 6. Chờ điều hướng sang /history
        cartPage.waitForUrlContains("/history");
        assertTrue(
                driver.getCurrentUrl().contains("/history"),
                "Sau khi Confirm and Pay thành công, hệ thống phải chuyển sang trang /history"
        );
    }
}
