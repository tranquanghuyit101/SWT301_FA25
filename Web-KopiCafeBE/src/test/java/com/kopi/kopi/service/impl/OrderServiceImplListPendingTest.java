package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
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
class OrderServiceImplListPendingTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderDetailAddOnRepository orderDetailAddOnRepository;

    @InjectMocks
    OrderServiceImpl orderService;

    @Test
    void listPending_tableType_calls_findByStatusNotInAndAddressIsNull_and_maps_table_number() {
        DiningTable table = DiningTable.builder().number(5).build();
        OrderEntity order = OrderEntity.builder().orderId(1).status("PENDING").createdAt(LocalDateTime.now()).build();
        order.setTable(table);
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1L);

        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(page);

        Map<String, Object> res = orderService.listPending("ANY", "TABLE", 1, 10);
        List<?> data = (List<?>) res.get("data");
        assertThat(data).hasSize(1);
        Map<?, ?> item = (Map<?, ?>) data.get(0);
        assertThat(item.get("table_number")).isEqualTo(5);
        assertThat(item.get("status")).isEqualTo("PENDING");
    }

    @Test
    void listPending_shippingType_calls_findByStatusNotInAndAddressIsNotNull_and_maps_address() {
        Address addr = Address.builder().addressLine("X Street").build();
        OrderEntity order = OrderEntity.builder().orderId(2).status("PENDING").createdAt(LocalDateTime.now()).build();
        order.setAddress(addr);
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1L);

        when(orderRepository.findByStatusNotInAndAddressIsNotNull(any(), any())).thenReturn(page);

        Map<String, Object> res = orderService.listPending("ANY", "SHIPPING", 1, 10);
        List<?> data = (List<?>) res.get("data");
        Map<?, ?> item = (Map<?, ?>) data.get(0);
        assertThat(item.get("address")).isEqualTo("X Street");
    }

    @Test
    void listPending_else_calls_findByStatus_and_returns_meta() {
        OrderEntity order = OrderEntity.builder().orderId(3).status("READY").createdAt(LocalDateTime.now()).build();
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(1, 10), 1L);
        when(orderRepository.findByStatus(any(), any())).thenReturn(page);

        Map<String, Object> res = orderService.listPending("READY", "OTHER", 2, 10);
        assertThat(res).containsKeys("data", "meta");
        Map<?, ?> meta = (Map<?, ?>) res.get("meta");
        assertThat(meta.get("currentPage")).isEqualTo(2);
    }

    @Test
    void listPending_products_and_addons_mapped_correctly() {
        Product prod = new Product();
        prod.setProductId(11);
        prod.setName("C");
        prod.setImgUrl("/c.png");
        OrderDetail d = OrderDetail.builder().orderDetailId(21).product(prod).productNameSnapshot("C").quantity(2)
                .lineTotal(new BigDecimal("20")).size(null).build();
        OrderEntity order = OrderEntity.builder().orderId(4).status("PENDING").createdAt(LocalDateTime.now()).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1L);

        OrderDetailAddOn oda = OrderDetailAddOn.builder().orderDetail(d)
                .addOn(AddOn.builder().addOnId(301).name("AO").build()).unitPriceSnapshot(new BigDecimal("3")).build();

        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(page);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(21)).thenReturn(List.of(oda));

        Map<String, Object> res = orderService.listPending("ANY", "TABLE", 1, 10);
        List<?> items = (List<?>) res.get("data");
        Map<?, ?> it = (Map<?, ?>) items.get(0);
        List<?> products = (List<?>) it.get("products");
        assertThat(products).hasSize(1);
        Map<?, ?> pd = (Map<?, ?>) products.get(0);
        assertThat(pd.get("product_name")).isEqualTo("C");
        List<?> addOns = (List<?>) pd.get("add_ons");
        assertThat(addOns).hasSize(1);
    }

    @Test
    void listPending_emptyPage_returns_empty_data_and_meta() {
        PageImpl<OrderEntity> empty = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0L);
        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(empty);

        Map<String, Object> res = orderService.listPending("ANY", "TABLE", 1, 10);
        List<?> data = (List<?>) res.get("data");
        Map<?, ?> meta = (Map<?, ?>) res.get("meta");
        assertThat(data).isEmpty();
        assertThat(meta.get("totalPage")).isEqualTo(0);
    }

    @Test
    void listPending_orderDetailsNull_products_empty_in_item() {
        OrderEntity order = OrderEntity.builder().orderId(5).status("PENDING").createdAt(LocalDateTime.now()).build();
        order.setOrderDetails(null);
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1L);
        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(page);

        Map<String, Object> res = orderService.listPending("ANY", "TABLE", 1, 10);
        List<?> data = (List<?>) res.get("data");
        Map<?, ?> it = (Map<?, ?>) data.get(0);
        List<?> products = (List<?>) it.get("products");
        assertThat(products).isEmpty();
    }

    @Test
    void listPending_maps_shipper_id_when_present() {
        User shipper = User.builder().userId(555).build();
        OrderEntity order = OrderEntity.builder().orderId(6).status("PENDING").createdAt(LocalDateTime.now()).build();
        order.setShipper(shipper);
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1L);
        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(page);

        Map<String, Object> res = orderService.listPending("ANY", "TABLE", 1, 10);
        List<?> data = (List<?>) res.get("data");
        Map<?, ?> it = (Map<?, ?>) data.get(0);
        assertThat(it.get("shipper_id")).isEqualTo(555);
    }

    @Test
    void listPending_maps_size_name_when_detail_has_size() {
        Size sz = Size.builder().sizeId(77).name("Medium").build();
        Product prod = new Product();
        prod.setProductId(2100);
        prod.setName("SProd");
        OrderDetail d = OrderDetail.builder().orderDetailId(210).product(prod).productNameSnapshot(null).quantity(1)
                .lineTotal(new BigDecimal("9")).size(sz).build();
        OrderEntity order = OrderEntity.builder().orderId(7).status("PENDING").createdAt(LocalDateTime.now()).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1L);

        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(page);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(210)).thenReturn(List.of());

        Map<String, Object> res = orderService.listPending("ANY", "TABLE", 1, 10);
        List<?> data = (List<?>) res.get("data");
        Map<?, ?> it = (Map<?, ?>) data.get(0);
        List<?> products = (List<?>) it.get("products");
        Map<?, ?> pd = (Map<?, ?>) products.get(0);
        assertThat(pd.get("size")).isEqualTo("Medium");
    }

    @Test
    void listPending_addon_name_and_price_mapped() {
        Product prod = new Product();
        prod.setProductId(2200);
        prod.setName("PA");
        OrderDetail d = OrderDetail.builder().orderDetailId(220).product(prod).productNameSnapshot(null).quantity(1)
                .lineTotal(new BigDecimal("5")).size(null).build();
        OrderEntity order = OrderEntity.builder().orderId(8).status("PENDING").createdAt(LocalDateTime.now()).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1L);

        OrderDetailAddOn oda = OrderDetailAddOn.builder().orderDetail(d)
                .addOn(AddOn.builder().addOnId(900).name("Sugar").build()).unitPriceSnapshot(new BigDecimal("2"))
                .build();
        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(page);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(220)).thenReturn(List.of(oda));

        Map<String, Object> res = orderService.listPending("ANY", "TABLE", 1, 10);
        List<?> data = (List<?>) res.get("data");
        Map<?, ?> it = (Map<?, ?>) data.get(0);
        List<?> products = (List<?>) it.get("products");
        Map<?, ?> pd = (Map<?, ?>) products.get(0);
        List<?> addOns = (List<?>) pd.get("add_ons");
        Map<?, ?> ao = (Map<?, ?>) addOns.get(0);
        assertThat(ao.get("name")).isEqualTo("Sugar");
        assertThat(ao.get("price")).isEqualTo(new BigDecimal("2"));
    }

    @Test
    void listPending_addon_null_and_snapshotNull_nameNull_priceZero() {
        Product prod = new Product();
        prod.setProductId(2300);
        prod.setName("PN");
        OrderDetail d = OrderDetail.builder().orderDetailId(230).product(prod).productNameSnapshot(null).quantity(1)
                .lineTotal(new BigDecimal("5")).size(null).build();
        OrderEntity order = OrderEntity.builder().orderId(9).status("PENDING").createdAt(LocalDateTime.now()).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1L);

        OrderDetailAddOn oda = OrderDetailAddOn.builder().orderDetail(d).addOn(null).unitPriceSnapshot(null).build();
        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(page);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(230)).thenReturn(List.of(oda));

        Map<String, Object> res = orderService.listPending("ANY", "TABLE", 1, 10);
        List<?> data = (List<?>) res.get("data");
        Map<?, ?> it = (Map<?, ?>) data.get(0);
        Map<?, ?> pd = (Map<?, ?>) ((List<?>) it.get("products")).get(0);
        List<?> addOns = (List<?>) pd.get("add_ons");
        Map<?, ?> ao = (Map<?, ?>) addOns.get(0);
        assertThat(ao.get("name")).isNull();
        assertThat(ao.get("price")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void listPending_addon_null_but_snapshotPrice_used() {
        Product prod = new Product();
        prod.setProductId(2400);
        prod.setName("PN2");
        OrderDetail d = OrderDetail.builder().orderDetailId(240).product(prod).productNameSnapshot(null).quantity(1)
                .lineTotal(new BigDecimal("5")).size(null).build();
        OrderEntity order = OrderEntity.builder().orderId(10).status("PENDING").createdAt(LocalDateTime.now()).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1L);

        OrderDetailAddOn oda = OrderDetailAddOn.builder().orderDetail(d).addOn(null)
                .unitPriceSnapshot(new BigDecimal("4")).build();
        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(page);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(240)).thenReturn(List.of(oda));

        Map<String, Object> res = orderService.listPending("ANY", "TABLE", 1, 10);
        List<?> data = (List<?>) res.get("data");
        Map<?, ?> it = (Map<?, ?>) data.get(0);
        Map<?, ?> pd = (Map<?, ?>) ((List<?>) it.get("products")).get(0);
        Map<?, ?> ao = (Map<?, ?>) ((List<?>) pd.get("add_ons")).get(0);
        assertThat(ao.get("name")).isNull();
        assertThat(ao.get("price")).isEqualTo(new BigDecimal("4"));
    }

    @Test
    void listPending_no_addons_returns_empty_addOns_list() {
        Product prod = new Product();
        prod.setProductId(2500);
        prod.setName("PN3");
        OrderDetail d = OrderDetail.builder().orderDetailId(250).product(prod).productNameSnapshot(null).quantity(1)
                .lineTotal(new BigDecimal("5")).size(null).build();
        OrderEntity order = OrderEntity.builder().orderId(11).status("PENDING").createdAt(LocalDateTime.now()).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));
        PageImpl<OrderEntity> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1L);

        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(page);
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(250)).thenReturn(List.of());

        Map<String, Object> res = orderService.listPending("ANY", "TABLE", 1, 10);
        List<?> data = (List<?>) res.get("data");
        Map<?, ?> it = (Map<?, ?>) data.get(0);
        Map<?, ?> pd = (Map<?, ?>) ((List<?>) it.get("products")).get(0);
        List<?> addOns = (List<?>) pd.get("add_ons");
        assertThat(addOns).isEmpty();
    }
}
