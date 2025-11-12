package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.PaymentMethod;
import com.kopi.kopi.repository.OrderDetailAddOnRepository;
import com.kopi.kopi.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplGetUserTransactionsTest {

        @Mock
        OrderRepository orderRepository;

        @Mock
        OrderDetailAddOnRepository orderDetailAddOnRepository;

        @InjectMocks
        OrderServiceImpl orderService;

        @Test
        void getUserTransactions_emptyPage_returnsEmptyData() {
                PageImpl<OrderEntity> empty = new PageImpl<OrderEntity>(Collections.emptyList(), PageRequest.of(0, 10),
                                0L);
                when(orderRepository.findByCustomer_UserId(anyInt(), any())).thenReturn(empty);

                var res = orderService.getUserTransactions(1, 1, 10);
                assertThat(res).containsKeys("data", "meta");
                List<?> data = (List<?>) res.get("data");
                assertThat(data).isEmpty();
        }

        @Test
        void getUserTransactions_withPayment_andAddress_mapsPaymentAndDelivery() {
                OrderEntity order = OrderEntity.builder().orderId(10).createdAt(LocalDateTime.now()).build();
                // payment present
                Payment p = Payment.builder().method(PaymentMethod.CASH).amount(new BigDecimal("5000")).build();
                order.getPayments().add(p);
                // address present
                Address a = Address.builder().addressLine("Addr 1").build();
                order.setAddress(a);
                order.setTotalAmount(new BigDecimal("6000"));

                PageImpl<OrderEntity> page = new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L);
                when(orderRepository.findByCustomer_UserId(anyInt(), any())).thenReturn(page);

                Map<String, Object> result = orderService.getUserTransactions(2, 1, 10);
                List<?> items = (List<?>) result.get("data");
                assertThat(items).hasSize(1);
                Map<?, ?> item = (Map<?, ?>) items.get(0);
                assertThat(item.get("payment_name")).isEqualTo("CASH");
                assertThat(item.get("delivery_address")).isEqualTo("Addr 1");
                assertThat(item.get("grand_total")).isNotNull();
        }

        @Test
        void getUserTransactions_withTable_deliveryNameIsTableNumber() {
                OrderEntity order = OrderEntity.builder().orderId(11).createdAt(LocalDateTime.now()).build();
                DiningTable table = DiningTable.builder().number(42).build();
                order.setTable(table);

                PageImpl<OrderEntity> page = new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L);
                when(orderRepository.findByCustomer_UserId(anyInt(), any())).thenReturn(page);

                Map<String, Object> result = orderService.getUserTransactions(3, 1, 10);
                List<?> items = (List<?>) result.get("data");
                Map<?, ?> item = (Map<?, ?>) items.get(0);
                assertThat(item.get("delivery_name")).isEqualTo("Table 42");
        }

        @Test
        void getUserTransactions_withDetails_andAddOns_mapsProductsAndAddOns() {
                // product and detail
                Product prod = new Product();
                prod.setProductId(100);
                prod.setName("Coffee");
                prod.setImgUrl("/coffee.png");
                OrderDetail d = OrderDetail.builder()
                                .orderDetailId(200)
                                .product(prod)
                                .productNameSnapshot(null)
                                .quantity(3)
                                .unitPrice(new BigDecimal("100"))
                                .build();
                // order with one detail
                OrderEntity order = OrderEntity.builder().orderId(12).createdAt(LocalDateTime.now()).build();
                order.setOrderDetails(new ArrayList<>(List.of(d)));

                // add-on for the detail
                AddOn ao = AddOn.builder().addOnId(300).name("Sugar").build();
                OrderDetailAddOn oda = OrderDetailAddOn.builder().orderDetail(d).addOn(ao)
                                .unitPriceSnapshot(new BigDecimal("5")).build();

                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));
                when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(200)).thenReturn(List.of(oda));

                Map<String, Object> res = orderService.getUserTransactions(4, 1, 10);
                List<?> items = (List<?>) res.get("data");
                assertThat(items).hasSize(1);
                Map<?, ?> it = (Map<?, ?>) items.get(0);
                List<?> products = (List<?>) it.get("products");
                assertThat(products).hasSize(1);
                Map<?, ?> pd = (Map<?, ?>) products.get(0);
                assertThat(pd.get("product_name")).isEqualTo("Coffee");
                assertThat(pd.get("product_img")).isEqualTo("/coffee.png");
                assertThat(pd.get("qty")).isEqualTo(3);
                List<?> addOns = (List<?>) pd.get("add_ons");
                assertThat(addOns).hasSize(1);
                Map<?, ?> aoMap = (Map<?, ?>) addOns.get(0);
                assertThat(aoMap.get("name")).isEqualTo("Sugar");
                assertThat(aoMap.get("price")).isEqualTo(new BigDecimal("5"));
        }

        @Test
        void getUserTransactions_paymentsEmpty_paymentNameNull() {
                OrderEntity order = OrderEntity.builder().orderId(20).createdAt(LocalDateTime.now()).build();
                // explicitly empty payments
                order.setPayments(new ArrayList<>());

                PageImpl<OrderEntity> page = new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L);
                when(orderRepository.findByCustomer_UserId(anyInt(), any())).thenReturn(page);

                Map<String, Object> res = orderService.getUserTransactions(5, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> item = (Map<?, ?>) items.get(0);
                // Expect payment_name to be null when payments list is empty
                assertThat(item.get("payment_name")).isNull();
        }

        @Test
        void getUserTransactions_detailProductNull_usesSnapshot() {
                OrderDetail d = OrderDetail.builder()
                                .orderDetailId(250)
                                .product(null)
                                .productNameSnapshot("Snapshot Product")
                                .quantity(1)
                                .unitPrice(new BigDecimal("10"))
                                .build();
                OrderEntity order = OrderEntity.builder().orderId(21).createdAt(LocalDateTime.now()).build();
                order.setOrderDetails(new ArrayList<>(List.of(d)));

                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));
                when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(250)).thenReturn(List.of());

                Map<String, Object> res = orderService.getUserTransactions(6, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> it = (Map<?, ?>) items.get(0);
                List<?> products = (List<?>) it.get("products");
                Map<?, ?> pd = (Map<?, ?>) products.get(0);
                assertThat(pd.get("product_name")).isEqualTo("Snapshot Product");
        }

        @Test
        void getUserTransactions_detailProductNull_snapshotNull_productNameNull() {
                OrderDetail d = OrderDetail.builder()
                                .orderDetailId(260)
                                .product(null)
                                .productNameSnapshot(null)
                                .quantity(1)
                                .unitPrice(new BigDecimal("10"))
                                .build();
                OrderEntity order = OrderEntity.builder().orderId(22).createdAt(LocalDateTime.now()).build();
                order.setOrderDetails(new ArrayList<>(List.of(d)));

                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));
                when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(260)).thenReturn(List.of());

                Map<String, Object> res = orderService.getUserTransactions(7, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> it = (Map<?, ?>) items.get(0);
                List<?> products = (List<?>) it.get("products");
                Map<?, ?> pd = (Map<?, ?>) products.get(0);
                assertThat(pd.get("product_name")).isNull();
        }

        @Test
        void getUserTransactions_paymentsNull_paymentNameNull() {
                OrderEntity order = OrderEntity.builder().orderId(30).createdAt(LocalDateTime.now()).build();
                order.setPayments(null);
                PageImpl<OrderEntity> page = new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L);
                when(orderRepository.findByCustomer_UserId(anyInt(), any())).thenReturn(page);

                Map<String, Object> res = orderService.getUserTransactions(8, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> item = (Map<?, ?>) items.get(0);
                // Expect payment_name to be null when payments is null
                assertThat(item.get("payment_name")).isNull();
        }

        @Test
        void getUserTransactions_noAddressNoTable_deliveryEmptyString() {
                OrderEntity order = OrderEntity.builder().orderId(31).createdAt(LocalDateTime.now()).build();
                order.setAddress(null);
                order.setTable(null);
                PageImpl<OrderEntity> page = new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L);
                when(orderRepository.findByCustomer_UserId(anyInt(), any())).thenReturn(page);

                Map<String, Object> res = orderService.getUserTransactions(9, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> item = (Map<?, ?>) items.get(0);
                assertThat(item.get("delivery_name")).isEqualTo("");
        }

        @Test
        void getUserTransactions_detailLineTotalNull_and_addOnNullHandled() {
                Product prod = new Product();
                prod.setProductId(4000);
                prod.setName("Nori");
                prod.setImgUrl(null);
                OrderDetail d = OrderDetail.builder().orderDetailId(400).product(prod).productNameSnapshot(null)
                                .quantity(1)
                                .lineTotal(null).unitPrice(new BigDecimal("10")).build();
                OrderEntity order = OrderEntity.builder().orderId(32).createdAt(LocalDateTime.now()).build();
                order.setOrderDetails(new ArrayList<>(List.of(d)));

                // add-on entry with null addOn and null unit price snapshot
                OrderDetailAddOn oda = OrderDetailAddOn.builder().orderDetail(d).addOn(null).unitPriceSnapshot(null)
                                .build();

                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));
                when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(400)).thenReturn(List.of(oda));

                Map<String, Object> res = orderService.getUserTransactions(10, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> it = (Map<?, ?>) items.get(0);
                List<?> products = (List<?>) it.get("products");
                Map<?, ?> pd = (Map<?, ?>) products.get(0);
                // lineTotal null -> subtotal becomes BigDecimal.ZERO
                assertThat(pd.get("subtotal")).isEqualTo(BigDecimal.ZERO);
                // product_img was null
                assertThat(pd.get("product_img")).isNull();
                // add_ons contains one element with name null and price 0
                List<?> addOns = (List<?>) pd.get("add_ons");
                Map<?, ?> aoMap = (Map<?, ?>) addOns.get(0);
                assertThat(aoMap.get("name")).isNull();
                assertThat(aoMap.get("price")).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        void getUserTransactions_paymentObjectWithNullMethod_paymentNameNull() {
                OrderEntity order = OrderEntity.builder().orderId(40).createdAt(LocalDateTime.now()).build();
                Payment p = Payment.builder().method(null).amount(new BigDecimal("1")).build();
                order.getPayments().add(p);
                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));

                Map<String, Object> res = orderService.getUserTransactions(11, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> item = (Map<?, ?>) items.get(0);
                // Expect payment_name to be null when payment method is null
                assertThat(item.get("payment_name")).isNull();
        }

        @Test
        void getUserTransactions_orderDetailsNull_productsEmpty() {
                OrderEntity order = OrderEntity.builder().orderId(41).createdAt(LocalDateTime.now()).build();
                order.setOrderDetails(null);
                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));

                Map<String, Object> res = orderService.getUserTransactions(12, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> item = (Map<?, ?>) items.get(0);
                List<?> products = (List<?>) item.get("products");
                assertThat(products).isEmpty();
        }

        @Test
        void getUserTransactions_detailWithSize_mapsSizeName() {
                Size sz = Size.builder().sizeId(5).name("Large").build();
                Product prod = new Product();
                prod.setProductId(5000);
                prod.setName("Tea");
                OrderDetail d = OrderDetail.builder().orderDetailId(500).product(prod).productNameSnapshot(null)
                                .quantity(1)
                                .unitPrice(new BigDecimal("20")).size(sz).build();
                OrderEntity order = OrderEntity.builder().orderId(42).createdAt(LocalDateTime.now()).build();
                order.setOrderDetails(new ArrayList<>(List.of(d)));

                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));
                when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(500)).thenReturn(List.of());

                Map<String, Object> res = orderService.getUserTransactions(13, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> it = (Map<?, ?>) items.get(0);
                List<?> products = (List<?>) it.get("products");
                Map<?, ?> pd = (Map<?, ?>) products.get(0);
                assertThat(pd.get("size")).isEqualTo("Large");
        }

        @Test
        void getUserTransactions_multiplePayments_usesFirstPaymentMethod() {
                OrderEntity order = OrderEntity.builder().orderId(60).createdAt(LocalDateTime.now()).build();
                Payment first = Payment.builder().method(PaymentMethod.BANKING).amount(new BigDecimal("10")).build();
                Payment second = Payment.builder().method(PaymentMethod.CASH).amount(new BigDecimal("5")).build();
                order.getPayments().add(first);
                order.getPayments().add(second);
                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));

                Map<String, Object> res = orderService.getUserTransactions(14, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> it = (Map<?, ?>) items.get(0);
                assertThat(it.get("payment_name")).isEqualTo("BANKING");
        }

        @Test
        void getUserTransactions_productSnapshotPreferredOverProductName() {
                Product prod = new Product();
                prod.setProductId(7000);
                prod.setName("RealName");
                prod.setImgUrl("/real.png");
                OrderDetail d = OrderDetail.builder().orderDetailId(700).product(prod)
                                .productNameSnapshot("SnapPreferred")
                                .quantity(1).unitPrice(new BigDecimal("1")).build();
                OrderEntity order = OrderEntity.builder().orderId(61).createdAt(LocalDateTime.now()).build();
                order.setOrderDetails(new ArrayList<>(List.of(d)));
                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));
                when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(700)).thenReturn(List.of());

                Map<String, Object> res = orderService.getUserTransactions(15, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> it = (Map<?, ?>) items.get(0);
                List<?> products = (List<?>) it.get("products");
                Map<?, ?> pd = (Map<?, ?>) products.get(0);
                assertThat(pd.get("product_name")).isEqualTo("SnapPreferred");
                assertThat(pd.get("product_img")).isEqualTo("/real.png");
        }

        @Test
        void getUserTransactions_productNull_productImgNull() {
                OrderDetail d = OrderDetail.builder().orderDetailId(800).product(null).productNameSnapshot(null)
                                .quantity(1)
                                .unitPrice(new BigDecimal("2")).build();
                OrderEntity order = OrderEntity.builder().orderId(62).createdAt(LocalDateTime.now()).build();
                order.setOrderDetails(new ArrayList<>(List.of(d)));
                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));
                when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(800)).thenReturn(List.of());

                Map<String, Object> res = orderService.getUserTransactions(16, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> it = (Map<?, ?>) items.get(0);
                List<?> products = (List<?>) it.get("products");
                Map<?, ?> pd = (Map<?, ?>) products.get(0);
                assertThat(pd.get("product_img")).isNull();
                assertThat(pd.get("product_name")).isNull();
        }

        @Test
        void getUserTransactions_addressAndTable_prefersShipping() {
                OrderEntity order = OrderEntity.builder().orderId(63).createdAt(LocalDateTime.now()).build();
                Address a = Address.builder().addressLine("AddrX").build();
                order.setAddress(a);
                DiningTable table = DiningTable.builder().number(77).build();
                order.setTable(table);
                when(orderRepository.findByCustomer_UserId(anyInt(), any()))
                                .thenReturn(new PageImpl<OrderEntity>(List.of(order), PageRequest.of(0, 10), 1L));

                Map<String, Object> res = orderService.getUserTransactions(17, 1, 10);
                List<?> items = (List<?>) res.get("data");
                Map<?, ?> it = (Map<?, ?>) items.get(0);
                assertThat(it.get("delivery_name")).isEqualTo("Shipping");
        }
}
