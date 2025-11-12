package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.PaymentMethod;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.repository.*;
import com.kopi.kopi.service.NotificationService;
import com.kopi.kopi.service.TableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderServiceImplChangeStatusTest {
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private TableService tableService;
    private NotificationService notificationService;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        AddressRepository addressRepository = mock(AddressRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        tableService = mock(TableService.class);
        DiningTableRepository diningTableRepository = mock(DiningTableRepository.class);
        UserAddressRepository userAddressRepository = mock(UserAddressRepository.class);
        MapboxService mapboxService = mock(MapboxService.class);
        ProductSizeRepository productSizeRepository = mock(ProductSizeRepository.class);
        ProductAddOnRepository productAddOnRepository = mock(ProductAddOnRepository.class);
        SizeRepository sizeRepository = mock(SizeRepository.class);
        OrderDetailAddOnRepository orderDetailAddOnRepository = mock(OrderDetailAddOnRepository.class);
        DiscountCodeRepository discountCodeRepository = mock(DiscountCodeRepository.class);
        DiscountCodeRedemptionRepository discountCodeRedemptionRepository = mock(
                DiscountCodeRedemptionRepository.class);
        notificationService = mock(NotificationService.class);

        orderService = new OrderServiceImpl(orderRepository, productRepository, addressRepository, userRepository,
                tableService, diningTableRepository, userAddressRepository, mapboxService, notificationService,
                productSizeRepository, productAddOnRepository, sizeRepository, orderDetailAddOnRepository,
                discountCodeRepository, discountCodeRedemptionRepository);
    }

    @Test
    void changeStatus_invalidStatus_returnsBadRequest() {
        ResponseEntity<?> resp = orderService.changeStatus(1, Map.of("status", "UNKNOWN"));
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat(resp.getBody()).isInstanceOf(Map.class);
    }

    @Test
    void changeStatus_completed_insufficientStock_returnsBadRequest() {
        Product p = new Product();
        p.setProductId(10);
        p.setName("TestProd");
        p.setStockQty(1);

        OrderDetail d = OrderDetail.builder()
                .order(null)
                .product(p)
                .quantity(2)
                .productNameSnapshot("TestProd")
                .unitPrice(BigDecimal.TEN)
                .orderDetailId(100)
                .build();

        OrderEntity order = OrderEntity.builder()
                .orderId(1)
                .status("PENDING")
                .orderDetails(List.of(d))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        d.setOrder(order);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        ResponseEntity<?> resp = orderService.changeStatus(1, Map.of("status", "COMPLETED"));
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertThat(String.valueOf(body.get("message"))).contains("không đủ số lượng");
    }

    @Test
    void changeStatus_completed_succeeds_and_deductsStock_and_saves() {
        Product p = new Product();
        p.setProductId(10);
        p.setName("GoodProd");
        p.setStockQty(5);

        OrderDetail d = OrderDetail.builder()
                .order(null)
                .product(p)
                .quantity(2)
                .productNameSnapshot("GoodProd")
                .unitPrice(BigDecimal.TEN)
                .orderDetailId(101)
                .build();

        OrderEntity order = OrderEntity.builder()
                .orderId(2)
                .status("PENDING")
                .orderDetails(List.of(d))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        d.setOrder(order);

        Payment payment = Payment.builder()
                .paymentId(55)
                .order(order)
                .amount(BigDecimal.TEN)
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        order.setPayments(List.of(payment));

        when(orderRepository.findById(2)).thenReturn(Optional.of(order));

        ResponseEntity<?> resp = orderService.changeStatus(2, Map.of("status", "COMPLETED"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        // verify product stock deducted and saved
        assertThat(p.getStockQty()).isEqualTo(3);
        verify(productRepository, times(1)).save(p);
        // payment status updated to PAID
        assertThat(order.getPayments().get(0).getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void changeStatus_updatesPaymentStatus_forCancelledAndPendingAndPaid() {
        OrderEntity order = OrderEntity.builder()
                .orderId(3)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Payment payment = Payment.builder()
                .paymentId(56)
                .order(order)
                .amount(BigDecimal.ONE)
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        order.setPayments(List.of(payment));

        when(orderRepository.findById(3)).thenReturn(Optional.of(order));

        // CANCELLED
        orderService.changeStatus(3, Map.of("status", "CANCELLED"));
        assertThat(order.getPayments().get(0).getStatus()).isEqualTo(PaymentStatus.CANCELLED);

        // PENDING
        orderService.changeStatus(3, Map.of("status", "PENDING"));
        assertThat(order.getPayments().get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);

        // PAID
        orderService.changeStatus(3, Map.of("status", "PAID"));
        assertThat(order.getPayments().get(0).getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void changeStatus_table_callsTableServiceSetAvailable() {
        DiningTable table = new DiningTable();
        table.setTableId(99);
        table.setNumber(5);

        OrderEntity order = OrderEntity.builder()
                .orderId(4)
                .status("PENDING")
                .table(table)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(4)).thenReturn(Optional.of(order));

        orderService.changeStatus(4, Map.of("status", "PAID"));
        verify(tableService, times(1)).setAvailableIfNoPendingOrders(99);
    }

    @Test
    void changeStatus_notificationException_isCaught_and_flow_continues() {
        OrderEntity order = OrderEntity.builder()
                .orderId(5)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        // make the notification service throw
        doThrow(new RuntimeException("notify fail")).when(notificationService)
                .notifyOrderStatusChangeToCustomer(any(), anyString(), anyString());
        doThrow(new RuntimeException("notify fail")).when(notificationService)
                .notifyOrderStatusChangeToStaff(any(), anyString(), anyString());

        // use the existing orderService instance (notificationService mock already
        // injected in setUp)
        ResponseEntity<?> resp = orderService.changeStatus(5, Map.of("status", "PAID"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }
}
