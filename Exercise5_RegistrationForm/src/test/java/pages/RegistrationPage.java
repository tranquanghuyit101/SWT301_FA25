package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

public class RegistrationPage extends BasePage {

    // 1. Constructor: Phải gọi super(driver) để BasePage có driver
    public RegistrationPage(WebDriver driver) {
        super(driver); // [cite: 163]
    }

    // 2. Locators: Định nghĩa tất cả các element bạn cần
    // Bạn cần tự tìm các ID, CSS, hoặc XPath cho trang demoqa.com [cite: 211]
    private By firstNameField = By.id("firstName");
    private By lastNameField = By.id("lastName");
    private By emailField = By.id("userEmail");
    private By genderMaleRadio = By.xpath("//label[text()='Male']");
    private By genderFemaleRadio = By.xpath("//label[text()='Female']");
    private By genderOtherRadio = By.xpath("//label[text()='Other']");
    private By mobileField = By.id("userNumber");
    private By dobField = By.id("dateOfBirthInput");
    private By subjectsField = By.id("subjectsInput");
    private By hobbiesSportsCheckbox = By.xpath("//label[text()='Sports']");
    private By hobbiesReadingCheckbox = By.xpath("//label[text()='Reading']");
    private By hobbiesMusicCheckbox = By.xpath("//label[text()='Music']");
    private By pictureUpload = By.id("uploadPicture");
    private By addressField = By.id("currentAddress");
    private By stateDropdown = By.id("state");
    private By cityDropdown = By.id("city");
    private By submitButton = By.id("submit");
    private By successModalHeader = By.id("example-modal-sizes-title-lg");
    private By closeModalButton = By.id("closeLargeModal");
    
    // Locators cho validation errors (thường có class "was-validated" hoặc border đỏ)
    private By firstNameFieldWrapper = By.cssSelector("#firstName");
    private By lastNameFieldWrapper = By.cssSelector("#lastName");
    private By emailFieldWrapper = By.cssSelector("#userEmail");
    private By mobileFieldWrapper = By.cssSelector("#userNumber");

    // 3. Actions: Các hàm để test class gọi đến

    /**
     * Điều hướng đến trang đăng ký
     */
    public void navigate() {
        // Sử dụng hàm tiện ích từ BasePage [cite: 166, 158]
        navigateTo("https://demoqa.com/automation-practice-form");
    }

    /**
     * Nhập thông tin cơ bản (phiên bản đơn giản)
     */
    public void fillForm(String firstName, String lastName, String email, String mobile, String address) {
        type(firstNameField, firstName);
        type(lastNameField, lastName);
        type(emailField, email);
        jsClick(genderMaleRadio);
        type(mobileField, mobile);
        type(addressField, address);
    }

    /**
     * Điền form đầy đủ với tất cả các trường
     */
    public void fillFullForm(String firstName, String lastName, String email, String mobile, 
                             String dateOfBirth, String subjects, String hobbies, 
                             String picturePath, String address, String state, String city) {
        type(firstNameField, firstName);
        type(lastNameField, lastName);
        type(emailField, email);
        jsClick(genderMaleRadio);
        type(mobileField, mobile);
        
        // Date of Birth
        if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
            fillDateOfBirth(dateOfBirth);
        }
        
        // Subjects
        if (subjects != null && !subjects.isEmpty()) {
            fillSubjects(subjects);
        }
        
        // Hobbies
        if (hobbies != null && !hobbies.isEmpty()) {
            selectHobbies(hobbies);
        }
        
        // Picture Upload
        if (picturePath != null && !picturePath.isEmpty()) {
            uploadPicture(picturePath);
        }
        
        type(addressField, address);
        
        // State and City
        if (state != null && !state.isEmpty()) {
            selectState(state);
        }
        if (city != null && !city.isEmpty()) {
            selectCity(city);
        }
    }

    /**
     * Điền Date of Birth
     */
    public void fillDateOfBirth(String date) {
        // Format: "15 May 1990"
        WebElement dobElement = waitForVisibility(dobField);
        dobElement.click();
        dobElement.sendKeys(Keys.CONTROL + "a");
        dobElement.sendKeys(date);
        dobElement.sendKeys(Keys.ENTER);
    }

    /**
     * Điền Subjects
     */
    public void fillSubjects(String subjects) {
        WebElement subjectsElement = waitForVisibility(subjectsField);
        subjectsElement.sendKeys(subjects);
        subjectsElement.sendKeys(Keys.ENTER);
    }

    /**
     * Chọn Hobbies
     */
    public void selectHobbies(String hobbies) {
        String[] hobbiesList = hobbies.split(",");
        for (String hobby : hobbiesList) {
            hobby = hobby.trim();
            if (hobby.equalsIgnoreCase("Sports")) {
                jsClick(hobbiesSportsCheckbox);
            } else if (hobby.equalsIgnoreCase("Reading")) {
                jsClick(hobbiesReadingCheckbox);
            } else if (hobby.equalsIgnoreCase("Music")) {
                jsClick(hobbiesMusicCheckbox);
            }
        }
    }

    /**
     * Upload picture
     */
    public void uploadPicture(String picturePath) {
        WebElement uploadElement = waitForVisibility(pictureUpload);
        File file = new File(picturePath);
        if (file.exists()) {
            uploadElement.sendKeys(file.getAbsolutePath());
        }
    }

    /**
     * Chọn State
     */
    public void selectState(String state) {
        // Dùng JavaScript click để tránh element bị che khuất
        jsClick(stateDropdown);
        // Chờ dropdown mở ra và option hiển thị
        try {
            Thread.sleep(300); // Chờ dropdown animation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // React Select sử dụng các class đặc biệt, thử nhiều cách
        By stateOption = By.xpath("//div[contains(@class, 'react-select')]//div[contains(@id, 'react-select') and text()='" + state + "'] | " +
                                   "//div[contains(@class, 'option') and text()='" + state + "'] | " +
                                   "//div[text()='" + state + "' and contains(@class, 'react-select')]");
        try {
            jsClick(stateOption);
        } catch (Exception e) {
            // Thử cách khác: tìm tất cả options và click vào option có text khớp
            By allOptions = By.xpath("//div[contains(@class, 'react-select')]//div[contains(@class, 'option')]");
            java.util.List<WebElement> options = driver.findElements(allOptions);
            for (WebElement option : options) {
                if (option.getText().equals(state)) {
                    // Dùng JavaScript click trực tiếp với WebElement
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
                    break;
                }
            }
        }
    }

    /**
     * Chọn City
     */
    public void selectCity(String city) {
        // Dùng JavaScript click để tránh element bị che khuất
        jsClick(cityDropdown);
        // Chờ dropdown mở ra và option hiển thị
        try {
            Thread.sleep(300); // Chờ dropdown animation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // React Select sử dụng các class đặc biệt, thử nhiều cách
        By cityOption = By.xpath("//div[contains(@class, 'react-select')]//div[contains(@id, 'react-select') and text()='" + city + "'] | " +
                                  "//div[contains(@class, 'option') and text()='" + city + "'] | " +
                                  "//div[text()='" + city + "' and contains(@class, 'react-select')]");
        try {
            jsClick(cityOption);
        } catch (Exception e) {
            // Thử cách khác: tìm tất cả options và click vào option có text khớp
            By allOptions = By.xpath("//div[contains(@class, 'react-select')]//div[contains(@class, 'option')]");
            java.util.List<WebElement> options = driver.findElements(allOptions);
            for (WebElement option : options) {
                if (option.getText().equals(city)) {
                    // Dùng JavaScript click trực tiếp với WebElement
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
                    break;
                }
            }
        }
    }

    /**
     * Điền từng trường riêng lẻ
     */
    public void fillFirstName(String firstName) {
        type(firstNameField, firstName);
    }

    public void fillLastName(String lastName) {
        type(lastNameField, lastName);
    }

    public void fillEmail(String email) {
        type(emailField, email);
    }

    public void selectGender(String gender) {
        if (gender.equalsIgnoreCase("Male")) {
            jsClick(genderMaleRadio);
        } else if (gender.equalsIgnoreCase("Female")) {
            jsClick(genderFemaleRadio);
        } else if (gender.equalsIgnoreCase("Other")) {
            jsClick(genderOtherRadio);
        }
    }

    public void fillMobile(String mobile) {
        type(mobileField, mobile);
    }

    /**
     * Nhấn nút submit
     */
    public void submit() {
        jsClick(submitButton);
    }

    /**
     * Kiểm tra validation error cho các trường
     */
    public boolean isFirstNameErrorVisible() {
        return hasValidationError(firstNameFieldWrapper);
    }

    public boolean isLastNameErrorVisible() {
        return hasValidationError(lastNameFieldWrapper);
    }

    public boolean isEmailErrorVisible() {
        return hasValidationError(emailFieldWrapper);
    }

    public boolean isMobileErrorVisible() {
        return hasValidationError(mobileFieldWrapper);
    }

    /**
     * Kiểm tra xem field có validation error không (thông qua CSS class, border, hoặc required attribute)
     */
    private boolean hasValidationError(By locator) {
        try {
            WebElement element = waitForVisibility(locator);
            
            // Kiểm tra required attribute và value rỗng
            String required = element.getAttribute("required");
            String value = element.getAttribute("value");
            if (required != null && (value == null || value.trim().isEmpty())) {
                return true;
            }
            
            String classValue = element.getAttribute("class");
            String styleValue = element.getAttribute("style");
            
            // Kiểm tra class có chứa "invalid" hoặc style có border đỏ
            boolean hasInvalidClass = classValue != null && 
                (classValue.contains("invalid") || classValue.contains("is-invalid"));
            
            // Kiểm tra border đỏ trong style
            boolean hasRedBorder = styleValue != null && 
                (styleValue.contains("border-color") && 
                 (styleValue.contains("rgb(220, 53, 69)") || 
                  styleValue.contains("#dc3545") ||
                  styleValue.contains("red")));
            
            if (hasInvalidClass || hasRedBorder) {
                return true;
            }
            
            // Kiểm tra parent element
            try {
                WebElement parent = element.findElement(By.xpath(".."));
                String parentClass = parent.getAttribute("class");
                if (parentClass != null && 
                    (parentClass.contains("invalid") || 
                     parentClass.contains("was-validated") ||
                     parentClass.contains("form-control-invalid"))) {
                    return true;
                }
            } catch (Exception ex) {
                // Ignore
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy locator của modal
     */
    public By getSuccessModalHeaderLocator() {
        return successModalHeader; // [cite: 168]
    }

    /**
     * Lấy text từ modal (nếu cần)
     */
    public String getModalHeaderText() {
        return getText(successModalHeader); // [cite: 170, 157]
    }

    /**
     * Kiểm tra xem modal thành công có hiển thị không
     */
    public boolean isSuccessModalVisible() {
        return isElementVisible(successModalHeader);
    }

    /**
     * Đóng modal thành công
     */
    public void closeSuccessModal() {
        if (isElementVisible(closeModalButton)) {
            jsClick(closeModalButton);
        }
    }

    /**
     * Lấy nội dung chi tiết từ modal (tất cả các dòng)
     */
    public String getModalContent() {
        try {
            By modalContent = By.cssSelector(".modal-body");
            return getText(modalContent);
        } catch (Exception e) {
            return "";
        }
    }
}