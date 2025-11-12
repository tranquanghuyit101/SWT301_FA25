package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.OrderDetailAddOnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class   OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderDetailAddOnRepository orderDetailAddOnRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private User testStaffUser;
    private OrderEntity testOrder;
    private Page<OrderEntity> testPage;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .fullName("Test User")
                .email("user@test.com")
                .build();

        testStaffUser = User.builder()
                .userId(2)
                .fullName("Staff User")
                .role(Role.builder().name("STAFF").build())
                .build();

        testOrder = OrderEntity.builder()
                .orderId(1001)
                .customer(testUser)
                .totalAmount(new BigDecimal("250.00"))
                .subtotalAmount(new BigDecimal("200.00"))
                .shippingAmount(new BigDecimal("30.00"))
                .discountAmount(new BigDecimal("20.00"))
                .status("COMPLETED")
                .note("Test note")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== HELPER METHODS ====================

    private void setupOrderWithPaymentsAndDetails() {
        // Setup payments
        Payment payment = Payment.builder()
                .paymentId(1)
                .method(com.kopi.kopi.entity.enums.PaymentMethod.BANKING)
                .build();
        testOrder.setPayments(List.of(payment));

        setupOrderDetails();
    }

    private void setupOrderDetails() {
        // Setup order details
        Product product = Product.builder()
                .productId(1)
                .name("Test Coffee")
                .imgUrl("coffee.jpg")
                .build();

        Size size = Size.builder()
                .sizeId(1)
                .name("Medium")
                .build();

        OrderDetail orderDetail = OrderDetail.builder()
                .orderDetailId(1)
                .product(product)
                .productNameSnapshot("Test Coffee")
                .quantity(2)
                .unitPrice(new BigDecimal("100.00"))
                .lineTotal(new BigDecimal("200.00"))
                .size(size)
                .build();

        testOrder.setOrderDetails(List.of(orderDetail));
    }

    // ==================== UNIT TEST CHO getUserTransactions ====================

    @Test
    void getUserTransactions_UTC001_NormalCase_UserWithTransactions() {
        // Given
        Integer userId = 1;
        Integer page = 1;
        Integer limit = 10;

        setupOrderWithPaymentsAndDetails();
        testPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("data"));
        assertTrue(result.containsKey("meta"));

        List<?> data = (List<?>) result.get("data");
        assertEquals(1, data.size());

        Map<?, ?> meta = (Map<?, ?>) result.get("meta");
        assertEquals(1, meta.get("currentPage"));
        assertEquals(1, meta.get("totalPage"));
        assertFalse((Boolean) meta.get("prev"));
        assertFalse((Boolean) meta.get("next"));

        verify(orderRepository).findByCustomer_UserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getUserTransactions_UTC002_NormalCase_PaginationPage2() {
        // Given
        Integer userId = 1;
        Integer page = 2;
        Integer limit = 5;

        setupOrderWithPaymentsAndDetails();
        testPage = new PageImpl<>(List.of(testOrder), PageRequest.of(1, 5), 15);

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);

        Map<?, ?> meta = (Map<?, ?>) result.get("meta");
        assertEquals(2, meta.get("currentPage"));
        assertEquals(3, meta.get("totalPage"));
        assertTrue((Boolean) meta.get("prev"));
        assertTrue((Boolean) meta.get("next"));
    }

    @Test
    void getUserTransactions_UTC003_BoundaryCase_PageZero() {
        // Given
        Integer userId = 1;
        Integer page = 0;
        Integer limit = 10;

        setupOrderWithPaymentsAndDetails();
        testPage = new PageImpl<>(Collections.emptyList());

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        Map<?, ?> meta = (Map<?, ?>) result.get("meta");
        assertEquals(1, meta.get("currentPage"));
    }

    @Test
    void getUserTransactions_UTC004_BoundaryCase_LimitOne() {
        // Given
        Integer userId = 1;
        Integer page = 1;
        Integer limit = 1;

        setupOrderWithPaymentsAndDetails();
        testPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        assertEquals(1, data.size());
    }

    @Test
    void getUserTransactions_UTC005_AbnormalCase_NegativeLimit() {
        // Given
        Integer userId = 1;
        Integer page = 1;
        Integer limit = -1;

        setupOrderWithPaymentsAndDetails();
        testPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        assertEquals(1, data.size());
    }

    @Test
    void getUserTransactions_UTC008_AbnormalCase_UserNoTransactions() {
        // Given
        Integer userId = 999;
        Integer page = 1;
        Integer limit = 10;

        testPage = new PageImpl<>(Collections.emptyList());

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        assertTrue(data.isEmpty());
        Map<?, ?> meta = (Map<?, ?>) result.get("meta");
        assertEquals(1, meta.get("currentPage"));
    }

    @Test
    void getUserTransactions_UTC009_AbnormalCase_NullInputs() {
        // Given
        Integer userId = null;
        Integer page = 1;
        Integer limit = 10;

        testPage = new PageImpl<>(Collections.emptyList());

        when(orderRepository.findByCustomer_UserId(isNull(), any(Pageable.class)))
                .thenReturn(testPage);

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        assertTrue(data.isEmpty());
    }

    @Test
    void getUserTransactions_OrderWithNullPayments() {
        // Given
        Integer userId = 1;
        Integer page = 1;
        Integer limit = 10;

        testOrder.setPayments(null);
        setupOrderDetails();
        testPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        Map<?, ?> firstOrder = (Map<?, ?>) data.get(0);
        assertNull(firstOrder.get("payment_name"));
    }

    @Test
    void getUserTransactions_OrderWithEmptyOrderDetails() {
        // Given
        Integer userId = 1;
        Integer page = 1;
        Integer limit = 10;

        testOrder.setOrderDetails(Collections.emptyList());
        testPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        Map<?, ?> firstOrder = (Map<?, ?>) data.get(0);
        List<?> products = (List<?>) firstOrder.get("products");
        assertTrue(products.isEmpty());
    }

    @Test
    void getUserTransactions_OrderWithDeliveryAddress() {
        // Given
        Integer userId = 1;
        Integer page = 1;
        Integer limit = 10;

        Address address = Address.builder()
                .addressId(1)
                .addressLine("123 Test Street")
                .build();
        testOrder.setAddress(address);
        testOrder.setTable(null);
        setupOrderWithPaymentsAndDetails();
        testPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        Map<?, ?> firstOrder = (Map<?, ?>) data.get(0);
        assertEquals("Shipping", firstOrder.get("delivery_name"));
        assertEquals("123 Test Street", firstOrder.get("delivery_address"));
    }

    @Test
    void getUserTransactions_OrderWithTableDelivery() {
        // Given
        Integer userId = 1;
        Integer page = 1;
        Integer limit = 10;

        DiningTable table = DiningTable.builder()
                .tableId(1)
                .number(5)
                .build();
        testOrder.setTable(table);
        testOrder.setAddress(null);
        setupOrderWithPaymentsAndDetails();
        testPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        Map<?, ?> firstOrder = (Map<?, ?>) data.get(0);
        assertEquals("Table 5", firstOrder.get("delivery_name"));
        assertNull(firstOrder.get("delivery_address"));
    }

    @Test
    void getUserTransactions_OrderWithAddOns() {
        // Given
        Integer userId = 1;
        Integer page = 1;
        Integer limit = 10;

        setupOrderWithPaymentsAndDetails();
        testPage = new PageImpl<>(List.of(testOrder));

        AddOn addOn = AddOn.builder()
                .addOnId(1)
                .name("Extra Shot")
                .build();

        OrderDetailAddOn orderDetailAddOn = OrderDetailAddOn.builder()
                .orderDetailAddOnId(1)
                .addOn(addOn)
                .unitPriceSnapshot(new BigDecimal("5.00"))
                .build();

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(List.of(orderDetailAddOn));

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        Map<?, ?> firstOrder = (Map<?, ?>) data.get(0);
        List<?> products = (List<?>) firstOrder.get("products");
        Map<?, ?> firstProduct = (Map<?, ?>) products.get(0);
        List<?> addOns = (List<?>) firstProduct.get("add_ons");

        assertEquals(1, addOns.size());
        Map<?, ?> firstAddOn = (Map<?, ?>) addOns.get(0);
        assertEquals("Extra Shot", firstAddOn.get("name"));
    }

    @Test
    void getUserTransactions_DefaultBigDecimalHandling() {
        // Given
        Integer userId = 1;
        Integer page = 1;
        Integer limit = 10;

        testOrder.setTotalAmount(null);
        testOrder.setSubtotalAmount(null);
        testOrder.setShippingAmount(null);
        testOrder.setDiscountAmount(null);
        setupOrderWithPaymentsAndDetails();

        OrderDetail orderDetail = testOrder.getOrderDetails().get(0);
        orderDetail.setLineTotal(null);

        testPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findByCustomer_UserId(eq(userId), any(Pageable.class)))
                .thenReturn(testPage);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = orderService.getUserTransactions(userId, page, limit);

        // Then
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        Map<?, ?> firstOrder = (Map<?, ?>) data.get(0);

        assertEquals(BigDecimal.ZERO, firstOrder.get("grand_total"));
        assertEquals(BigDecimal.ZERO, firstOrder.get("subtotal"));
        assertEquals(BigDecimal.ZERO, firstOrder.get("shipping_fee"));
        assertEquals(BigDecimal.ZERO, firstOrder.get("discount"));

        List<?> products = (List<?>) firstOrder.get("products");
        Map<?, ?> firstProduct = (Map<?, ?>) products.get(0);
        assertEquals(BigDecimal.ZERO, firstProduct.get("subtotal"));
    }

    // ==================== UNIT TEST CHO getTransactionDetail ====================

    @Test
    void getTransactionDetail_UTC001_NormalCase_OwnerAccess() {
        // Given
        Integer orderId = 1001;
        User currentUser = testUser;

        setupOrderWithPaymentsAndDetails();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.containsKey("data"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        assertEquals(1, data.size());

        Map<String, Object> detail = data.get(0);
        assertEquals(1001, detail.get("id"));
        assertEquals("Test User", detail.get("receiver_name"));
        assertEquals("COMPLETED", detail.get("status_name"));

        verify(orderRepository).findById(orderId);
    }

    @Test
    void getTransactionDetail_UTC002_NormalCase_StaffAdminAccess() {
        // Given
        Integer orderId = 1001;
        User adminUser = User.builder()
                .userId(3)
                .role(Role.builder().name("ADMIN").build())
                .build();

        setupOrderWithPaymentsAndDetails();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, adminUser);

        // Then
        assertEquals(200, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        assertFalse(data.isEmpty());
    }

    @Test
    void getTransactionDetail_UTC003_NormalCase_StaffEmployeeAccess() {
        // Given
        Integer orderId = 1001;
        User employeeUser = User.builder()
                .userId(4)
                .role(Role.builder().name("EMPLOYEE").build())
                .build();

        setupOrderWithPaymentsAndDetails();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, employeeUser);

        // Then
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void getTransactionDetail_UTC005_NormalCase_OrderWithDeliveryAddress() {
        // Given
        Integer orderId = 1001;
        User currentUser = testStaffUser;

        Address address = Address.builder()
                .addressId(1)
                .addressLine("123 Test Street, Da Nang")
                .build();
        testOrder.setAddress(address);
        testOrder.setTable(null);

        setupOrderWithPaymentsAndDetails();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);

        // Then
        assertEquals(200, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        Map<String, Object> detail = data.get(0);

        assertEquals("Shipping", detail.get("delivery_name"));
        assertEquals("123 Test Street, Da Nang", detail.get("delivery_address"));
    }

//    @Test
//    void getTransactionDetail_UTC006_NormalCase_OrderWithTableDelivery() {
//        // Given
//        Integer orderId = 1001;
//        User currentUser = testStaffUser;
//
//        DiningTable table = DiningTable.builder()
//                .tableId(1)
//                .number(5)
//                .build();
//        testOrder.setTable(table);
//        testOrder.setAddress(null);
//
//        setupOrderWithPaymentsAndDetails();
//        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
//        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
//                .thenReturn(Collections.emptyList());
//
//        // When
//        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);
//
//        // Then
//        assertEquals(200, response.getStatusCodeValue());
//
//        @SuppressWarnings("unchecked")
//        Map<String, Object> body = (Map<String, Object>) response.getBody();
//        @SuppressWarnings("unchecked")
//        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
//        Map<String, Object> detail = data.get(0);
//
//        assertEquals("Table 5", detail.get("delivery_name"));
//        assertNull(detail.get("delivery_address"));
//    }

    @Test
    void getTransactionDetail_UTC007_NormalCase_OrderWithProductsAndAddOns() {
        // Given
        Integer orderId = 1001;
        User currentUser = testStaffUser;

        setupOrderWithPaymentsAndDetails();

        AddOn addOn = AddOn.builder()
                .addOnId(1)
                .name("Extra Shot")
                .build();

        OrderDetailAddOn orderDetailAddOn = OrderDetailAddOn.builder()
                .orderDetailAddOnId(1)
                .addOn(addOn)
                .unitPriceSnapshot(new BigDecimal("5.00"))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(List.of(orderDetailAddOn));

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);

        // Then
        assertEquals(200, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        Map<String, Object> detail = data.get(0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) detail.get("products");
        assertFalse(products.isEmpty());

        Map<String, Object> product = products.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> addOns = (List<Map<String, Object>>) product.get("add_ons");
        assertFalse(addOns.isEmpty());
        assertEquals("Extra Shot", addOns.get(0).get("name"));
    }

    @Test
    void getTransactionDetail_UTC008_NormalCase_OrderWithoutProducts() {
        // Given
        Integer orderId = 1001;
        User currentUser = testStaffUser;

        testOrder.setOrderDetails(Collections.emptyList());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);

        // Then
        assertEquals(200, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        Map<String, Object> detail = data.get(0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) detail.get("products");
        assertTrue(products.isEmpty());
    }

    @Test
    void getTransactionDetail_UTC009_BoundaryCase_OrderNotFound() {
        // Given
        Integer orderId = 9999;
        User currentUser = testUser;

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NoSuchElementException.class, () -> {
            orderService.getTransactionDetail(orderId, currentUser);
        });
    }

    @Test
    void getTransactionDetail_UTC010_BoundaryCase_CurrentUserNull() {
        // Given
        Integer orderId = 1001;
        User currentUser = null;

        setupOrderWithPaymentsAndDetails();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);

        // Then
        assertEquals(403, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Forbidden", body.get("message"));
    }

    @Test
    void getTransactionDetail_UTC011_AbnormalCase_UnauthorizedAccess() {
        // Given
        Integer orderId = 1001;
        User unauthorizedUser = User.builder()
                .userId(999)
                .role(Role.builder().name("CUSTOMER").build())
                .build();

        testOrder.setCustomer(User.builder().userId(1).build());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, unauthorizedUser);

        // Then
        assertEquals(403, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Forbidden", body.get("message"));
    }

    @Test
    void getTransactionDetail_UTC012_AbnormalCase_OrderWithoutCustomer() {
        // Given
        Integer orderId = 1001;
        User currentUser = testStaffUser;

        testOrder.setCustomer(null);
        setupOrderWithPaymentsAndDetails();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);

        // Then
        assertEquals(200, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        Map<String, Object> detail = data.get(0);

        assertEquals("", detail.get("receiver_name"));
    }

//    @Test
//    void getTransactionDetail_UTC013_AbnormalCase_OrderWithoutPayments() {
//        // Given
//        Integer orderId = 1001;
//        User currentUser = testStaffUser;
//
//        testOrder.setPayments(null);
//        setupOrderWithPaymentsAndDetails();
//
//        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
//        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
//                .thenReturn(Collections.emptyList());
//
//        // When
//        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);
//
//        // Then
//        assertEquals(200, response.getStatusCodeValue());
//
//        @SuppressWarnings("unchecked")
//        Map<String, Object> body = (Map<String, Object>) response.getBody();
//        @SuppressWarnings("unchecked")
//        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
//        Map<String, Object> detail = data.get(0);
//
//        assertNull(detail.get("payment_name"));
//    }

    @Test
    void getTransactionDetail_OrderWithNullOrderDetails() {
        // Given
        Integer orderId = 1001;
        User currentUser = testStaffUser;

        testOrder.setOrderDetails(null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);

        // Then
        assertEquals(200, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        Map<String, Object> detail = data.get(0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) detail.get("products");
        assertTrue(products.isEmpty());
    }

//    @Test
//    void getTransactionDetail_OrderWithEmptyPayments() {
//        // Given
//        Integer orderId = 1001;
//        User currentUser = testStaffUser;
//
//        testOrder.setPayments(Collections.emptyList());
//        setupOrderWithPaymentsAndDetails();
//
//        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
//        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
//                .thenReturn(Collections.emptyList());
//
//        // When
//        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);
//
//        // Then
//        assertEquals(200, response.getStatusCodeValue());
//
//        @SuppressWarnings("unchecked")
//        Map<String, Object> body = (Map<String, Object>) response.getBody();
//        @SuppressWarnings("unchecked")
//        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
//        Map<String, Object> detail = data.get(0);
//
//        assertNull(detail.get("payment_name"));
//    }

    @Test
    void getTransactionDetail_StaffWithNullRole() {
        // Given
        Integer orderId = 1001;
        User staffWithNullRole = User.builder()
                .userId(5)
                .role(null)
                .build();

        testOrder.setCustomer(User.builder().userId(1).build());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, staffWithNullRole);

        // Then
        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void getTransactionDetail_DefaultBigDecimalHandling() {
        // Given
        Integer orderId = 1001;
        User currentUser = testStaffUser;

        testOrder.setTotalAmount(null);
        testOrder.setSubtotalAmount(null);
        testOrder.setShippingAmount(null);
        testOrder.setDiscountAmount(null);

        setupOrderWithPaymentsAndDetails();
        OrderDetail orderDetail = testOrder.getOrderDetails().get(0);
        orderDetail.setLineTotal(null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = orderService.getTransactionDetail(orderId, currentUser);

        // Then
        assertEquals(200, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        Map<String, Object> detail = data.get(0);

        assertEquals(BigDecimal.ZERO, detail.get("grand_total"));
        assertEquals(BigDecimal.ZERO, detail.get("subtotal"));
        assertEquals(BigDecimal.ZERO, detail.get("delivery_fee"));
        assertEquals(BigDecimal.ZERO, detail.get("discount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) detail.get("products");
        Map<String, Object> product = products.get(0);
        assertEquals(BigDecimal.ZERO, product.get("subtotal"));
    }
}