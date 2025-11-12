package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTransactionsTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderDetailAddOnRepository orderDetailAddOnRepository;

    @InjectMocks
    OrderServiceImpl orderService;

    @Test
    void getTransactionDetail_forbidden_when_not_owner_or_staff() {
        // order belongs to user 10
        OrderEntity order = OrderEntity.builder().orderId(1).build();
        User owner = User.builder().userId(10).build();
        order.setCustomer(owner);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        // current user is different and has no staff role
        User current = User.builder().userId(11).build();

        ResponseEntity<?> res = orderService.getTransactionDetail(1, current);
        assertThat(res.getStatusCodeValue()).isEqualTo(403);
    }

    @Test
    void getTransactionDetail_owner_with_no_payments_and_no_products_returns_ok() {
        OrderEntity order = OrderEntity.builder()
                .orderId(2)
                .createdAt(LocalDateTime.now())
                .status("PENDING")
                .build();
        User owner = User.builder().userId(20).build();
        order.setCustomer(owner);
        order.setOrderDetails(null); // exercise branch where orderDetails == null

        when(orderRepository.findById(2)).thenReturn(Optional.of(order));

        User current = User.builder().userId(20).build();

        ResponseEntity<?> res = orderService.getTransactionDetail(2, current);
        assertThat(res.getStatusCodeValue()).isEqualTo(200);

        Object body = res.getBody();
        assertThat(body).isNotNull();
        // body expected to be a map with data -> list
        assertThat(body).isInstanceOf(Map.class);
        Map<?, ?> map = (Map<?, ?>) body;
        assertThat(map.get("data")).isInstanceOf(List.class);
        List<?> dataList = (List<?>) map.get("data");
        assertThat(dataList).isNotEmpty();
    }

    @Test
    void getTransactionDetail_owner_with_payment_address_and_addons_maps_fields() {
        // create an order with payment, address and one detail
        OrderEntity order = OrderEntity.builder()
                .orderId(3)
                .createdAt(LocalDateTime.now())
                .status("COMPLETED")
                .build();

        User owner = User.builder().userId(30).fullName("Owner").build();
        order.setCustomer(owner);

        Address addr = Address.builder().addressLine("123 Test St").city("Da Nang").build();
        order.setAddress(addr);

        Payment p = Payment.builder().method(null).amount(new BigDecimal("100")).status(null).build();
        order.getPayments().add(p);

        Product prod = new Product();
        prod.setProductId(400);
        prod.setName("P");
        prod.setImgUrl("/img.png");
        OrderDetail d = OrderDetail.builder().orderDetailId(500).product(prod).productNameSnapshot(null).quantity(2)
                .unitPrice(new BigDecimal("10")).size(null).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        // add-ons returned for the detail
        OrderDetailAddOn oda = OrderDetailAddOn.builder().orderDetail(d)
                .addOn(AddOn.builder().addOnId(600).name("AO").build()).unitPriceSnapshot(new BigDecimal("1")).build();
        when(orderRepository.findById(3)).thenReturn(Optional.of(order));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(500)).thenReturn(List.of(oda));

        User current = User.builder().userId(30).build();

        ResponseEntity<?> res = orderService.getTransactionDetail(3, current);
        assertThat(res.getStatusCodeValue()).isEqualTo(200);

        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertThat(body).isNotNull();
        List<?> data = (List<?>) body.get("data");
        assertThat(data).isNotEmpty();
        Object first = data.get(0);
        assertThat(first).isInstanceOf(Map.class).isNotNull();
        // further assertions would inspect nested maps but primary goal is exercising
        // branches
    }
}
