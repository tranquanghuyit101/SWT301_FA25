package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.DiscountCode;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.entity.enums.DiscountType;
import com.kopi.kopi.repository.*;
import com.kopi.kopi.service.NotificationService;
import com.kopi.kopi.service.TableService;
import com.kopi.kopi.service.impl.MapboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceImplValidateDiscountTest {
    private OrderServiceImpl svc;

    private OrderRepository orderRepository = mock(OrderRepository.class);
    private ProductRepository productRepository = mock(ProductRepository.class);
    private AddressRepository addressRepository = mock(AddressRepository.class);
    private UserRepository userRepository = mock(UserRepository.class);
    private TableService tableService = mock(TableService.class);
    private com.kopi.kopi.repository.DiningTableRepository diningTableRepository = mock(
            com.kopi.kopi.repository.DiningTableRepository.class);
    private UserAddressRepository userAddressRepository = mock(UserAddressRepository.class);
    private MapboxService mapboxService = mock(MapboxService.class);
    private ProductSizeRepository productSizeRepository = mock(ProductSizeRepository.class);
    private ProductAddOnRepository productAddOnRepository = mock(ProductAddOnRepository.class);
    private SizeRepository sizeRepository = mock(SizeRepository.class);
    private OrderDetailAddOnRepository orderDetailAddOnRepository = mock(OrderDetailAddOnRepository.class);
    private DiscountCodeRepository discountCodeRepository = mock(DiscountCodeRepository.class);
    private DiscountCodeRedemptionRepository discountCodeRedemptionRepository = mock(
            DiscountCodeRedemptionRepository.class);
    private NotificationService notificationService = mock(NotificationService.class);

    @BeforeEach
    void setUp() {
        svc = new OrderServiceImpl(orderRepository, productRepository, addressRepository, userRepository,
                tableService, diningTableRepository, userAddressRepository, mapboxService, notificationService,
                productSizeRepository, productAddOnRepository, sizeRepository, orderDetailAddOnRepository,
                discountCodeRepository, discountCodeRedemptionRepository);
    }

    @Test
    void validateDiscount_emptyCode_returnsBadRequest() {
        ResponseEntity<?> r = svc.validateDiscount(Map.of(), null);
        assertThat(r.getStatusCodeValue()).isEqualTo(400);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("Vui lòng nhập mã giảm giá");
    }

    @Test
    void validateDiscount_codeNotExist_returnsBadRequest() {
        when(discountCodeRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        ResponseEntity<?> r = svc.validateDiscount(Map.of("code", "NOPE"), null);
        assertThat(r.getStatusCodeValue()).isEqualTo(400);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("Mã giảm giá không tồn tại");
    }

    @Test
    void validateDiscount_inactiveCode_returnsBadRequest() {
        DiscountCode dc = DiscountCode.builder()
                .code("X")
                .active(false)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .discountType(DiscountType.AMOUNT)
                .discountValue(new BigDecimal("100"))
                .build();
        when(discountCodeRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.of(dc));
        ResponseEntity<?> r = svc.validateDiscount(Map.of("code", "X", "subtotal", "200"), null);
        assertThat(r.getStatusCodeValue()).isEqualTo(400);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("Mã giảm giá đã bị vô hiệu hoá");
    }

    @Test
    void validateDiscount_minOrderNotReached_returnsBadRequest() {
        DiscountCode dc = DiscountCode.builder()
                .code("MIN")
                .active(true)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .discountType(DiscountType.AMOUNT)
                .discountValue(new BigDecimal("50"))
                .minOrderAmount(new BigDecimal("100"))
                .build();
        when(discountCodeRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.of(dc));
        ResponseEntity<?> r = svc.validateDiscount(Map.of("code", "MIN", "subtotal", "50"), mock(User.class));
        assertThat(r.getStatusCodeValue()).isEqualTo(400);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("Chưa đạt giá trị đơn tối thiểu");
    }

    @Test
    void validateDiscount_percentBiggerThanSubtotal_capsToSubtotal() {
        DiscountCode dc = DiscountCode.builder()
                .code("PCT")
                .active(true)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("200"))
                .build();
        when(discountCodeRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.of(dc));
        ResponseEntity<?> r = svc.validateDiscount(Map.of("code", "PCT", "subtotal", "50"), mock(User.class));
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        Map<?, ?> body = (Map<?, ?>) r.getBody();
        assertThat(body.get("valid")).isEqualTo(Boolean.TRUE);
        // discount_amount should be capped to subtotal (50)
        assertThat(new BigDecimal(String.valueOf(body.get("discount_amount"))))
                .isEqualByComparingTo(new BigDecimal("50"));
    }
}
