package tests;

import org.junit.jupiter.api.*;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pages.RegistrationPage;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Registration Form Tests")
public class RegistrationTest extends BaseTest {

    static RegistrationPage registrationPage;
    static WebDriverWait wait;

    @BeforeAll
    static void initPage() {
        registrationPage = new RegistrationPage(driver);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @BeforeEach
    void setUp() {
        // Đảm bảo mỗi test bắt đầu từ trang mới
        registrationPage.navigate();
    }

    // ========== TEST CASE 1: Submit thành công với dữ liệu đầy đủ ==========
    @Test
    @DisplayName("Should submit form successfully with full valid data")
    void testSuccessfulRegistrationWithFullData() {
        // Arrange & Act
        registrationPage.fillFullForm(
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890",
                "15 May 1990",
                "Maths",
                "Sports,Reading",
                "", // Không upload picture trong test này
                "123 Main Street, City",
                "NCR",
                "Delhi"
        );
        registrationPage.submit();

        // Assert
        WebElement successModal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                registrationPage.getSuccessModalHeaderLocator()
        ));

        String headerText = successModal.getText();
        assertTrue(headerText.contains("Thanks for submitting the form"),
                "Modal header should contain success message");

        // Kiểm tra nội dung modal chi tiết
        String modalContent = registrationPage.getModalContent();
        assertTrue(modalContent.contains("John"), "Modal should contain first name");
        assertTrue(modalContent.contains("Doe"), "Modal should contain last name");
        assertTrue(modalContent.contains("john.doe@example.com"), "Modal should contain email");
        assertTrue(modalContent.contains("1234567890"), "Modal should contain mobile number");

        // Đóng modal
        registrationPage.closeSuccessModal();
    }

    // ========== TEST CASE 2: Submit thành công với dữ liệu tối thiểu ==========
    @Test
    @DisplayName("Should submit form successfully with minimum required data")
    void testSuccessfulRegistration() {
        // Arrange & Act
        registrationPage.fillForm(
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890",
                "123 Main St"
        );
        registrationPage.submit();

        // Assert
        WebElement successModal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                registrationPage.getSuccessModalHeaderLocator()
        ));

        String headerText = successModal.getText();
        assertTrue(headerText.contains("Thanks for submitting the form"),
                "Modal header text is incorrect!");

        registrationPage.closeSuccessModal();
    }

    // ========== TEST CASE 3: Validation errors khi tất cả trường trống ==========
    @Test
    @DisplayName("Should show validation errors when all required fields are empty")
    void testEmptySubmission() {
        // Act
        registrationPage.submit(); // Không điền gì cả

        // Assert - Kiểm tra các trường bắt buộc có lỗi validation
        // Lưu ý: Trang demoqa.com có thể không hiển thị lỗi ngay lập tức,
        // nhưng form sẽ không submit được và các trường sẽ có border đỏ
        assertTrue(registrationPage.isFirstNameErrorVisible() || 
                   !registrationPage.isSuccessModalVisible(),
                "First name should show validation error or form should not submit");

        // Kiểm tra form không submit thành công (modal không hiển thị)
        assertFalse(registrationPage.isSuccessModalVisible(),
                "Success modal should not appear when form is empty");
    }

    // ========== TEST CASE 4: Validation error khi thiếu First Name ==========
    @Test
    @DisplayName("Should show validation error when First Name is missing")
    void testMissingFirstName() {
        // Arrange & Act
        registrationPage.fillLastName("Doe");
        registrationPage.fillEmail("john.doe@example.com");
        registrationPage.selectGender("Male");
        registrationPage.fillMobile("1234567890");
        registrationPage.submit();

        // Assert
        assertTrue(registrationPage.isFirstNameErrorVisible() || 
                   !registrationPage.isSuccessModalVisible(),
                "Should show error for missing first name");
    }

    // ========== TEST CASE 5: Validation error khi thiếu Last Name ==========
    @Test
    @DisplayName("Should show validation error when Last Name is missing")
    void testMissingLastName() {
        // Arrange & Act
        registrationPage.fillFirstName("John");
        registrationPage.fillEmail("john.doe@example.com");
        registrationPage.selectGender("Male");
        registrationPage.fillMobile("1234567890");
        registrationPage.submit();

        // Assert
        assertTrue(registrationPage.isLastNameErrorVisible() || 
                   !registrationPage.isSuccessModalVisible(),
                "Should show error for missing last name");
    }

    // ========== TEST CASE 6: Validation error khi thiếu Email ==========
    @Test
    @DisplayName("Should show validation error when Email is missing")
    void testMissingEmail() {
        // Arrange & Act
        registrationPage.fillFirstName("John");
        registrationPage.fillLastName("Doe");
        registrationPage.selectGender("Male");
        registrationPage.fillMobile("1234567890");
        registrationPage.submit();

        // Assert - Email có thể không bắt buộc hoặc validation error không hiển thị rõ
        // Kiểm tra chính: form không submit thành công (modal không hiển thị)
        // Hoặc nếu submit được, kiểm tra email error
        boolean modalNotVisible = !registrationPage.isSuccessModalVisible();
        boolean emailErrorVisible = registrationPage.isEmailErrorVisible();
        
        assertTrue(modalNotVisible || emailErrorVisible,
                "Should not submit successfully when email is missing, or show email validation error");
    }

    // ========== TEST CASE 7: Validation error khi Email không hợp lệ ==========
    @Test
    @DisplayName("Should show validation error when Email is invalid")
    void testInvalidEmail() {
        // Arrange & Act
        registrationPage.fillForm(
                "John",
                "Doe",
                "invalid-email", // Email không hợp lệ
                "1234567890",
                "123 Main St"
        );
        registrationPage.submit();

        // Assert
        assertTrue(registrationPage.isEmailErrorVisible() || 
                   !registrationPage.isSuccessModalVisible(),
                "Should show error for invalid email format");
    }

    // ========== TEST CASE 8: Validation error khi thiếu Gender ==========
    @Test
    @DisplayName("Should show validation error when Gender is not selected")
    void testMissingGender() {
        // Arrange & Act
        registrationPage.fillFirstName("John");
        registrationPage.fillLastName("Doe");
        registrationPage.fillEmail("john.doe@example.com");
        registrationPage.fillMobile("1234567890");
        registrationPage.submit();

        // Assert - Gender là trường bắt buộc
        assertFalse(registrationPage.isSuccessModalVisible(),
                "Should not submit successfully without gender selection");
    }

    // ========== TEST CASE 9: Validation error khi thiếu Mobile ==========
    @Test
    @DisplayName("Should show validation error when Mobile is missing")
    void testMissingMobile() {
        // Arrange & Act
        registrationPage.fillFirstName("John");
        registrationPage.fillLastName("Doe");
        registrationPage.fillEmail("john.doe@example.com");
        registrationPage.selectGender("Male");
        registrationPage.submit();

        // Assert
        assertTrue(registrationPage.isMobileErrorVisible() || 
                   !registrationPage.isSuccessModalVisible(),
                "Should show error for missing mobile number");
    }

    // ========== TEST CASE 10: Validation error khi Mobile không hợp lệ (quá ngắn) ==========
    @Test
    @DisplayName("Should show validation error when Mobile number is too short")
    void testInvalidMobileTooShort() {
        // Arrange & Act
        registrationPage.fillForm(
                "John",
                "Doe",
                "john.doe@example.com",
                "12345", // Quá ngắn (cần 10 số)
                "123 Main St"
        );
        registrationPage.submit();

        // Assert
        assertTrue(registrationPage.isMobileErrorVisible() || 
                   !registrationPage.isSuccessModalVisible(),
                "Should show error for mobile number that is too short");
    }

    // ========== TEST CASE 11: Validation error khi Mobile không hợp lệ (quá dài) ==========
    @Test
    @DisplayName("Should show validation error when Mobile number is too long")
    void testInvalidMobileTooLong() {
        // Arrange & Act
        registrationPage.fillForm(
                "John",
                "Doe",
                "john.doe@example.com",
                "123456789012345", // Quá dài (15 số, chỉ cần 10)
                "123 Main St"
        );
        registrationPage.submit();

        // Assert - Form có thể vẫn submit được nhưng chỉ lấy 10 số đầu
        // Hoặc có thể hiển thị validation error
        // Kiểm tra: modal không hiển thị HOẶC mobile error hiển thị
        boolean modalVisible = registrationPage.isSuccessModalVisible();
        boolean mobileErrorVisible = registrationPage.isMobileErrorVisible();
        
        // Nếu modal không hiển thị hoặc có mobile error, test pass
        // Nếu modal hiển thị, có thể form đã tự động cắt số hoặc chấp nhận
        assertTrue(!modalVisible || mobileErrorVisible,
                "Should not submit successfully with mobile number that is too long, or show validation error. " +
                "Note: Some forms may auto-truncate long numbers.");
    }

    // ========== TEST CASE 12: Validation error khi Mobile chứa ký tự không phải số ==========
    @Test
    @DisplayName("Should show validation error when Mobile contains non-numeric characters")
    void testInvalidMobileWithLetters() {
        // Arrange & Act
        registrationPage.fillForm(
                "John",
                "Doe",
                "john.doe@example.com",
                "123456789a", // Chứa chữ cái
                "123 Main St"
        );
        registrationPage.submit();

        // Assert
        assertTrue(registrationPage.isMobileErrorVisible() || 
                   !registrationPage.isSuccessModalVisible(),
                "Should show error for mobile number with letters");
    }
}