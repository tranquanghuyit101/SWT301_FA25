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
import org.springframework.http.ResponseEntity;
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
class OrderServiceImplGetTransactionDetailTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderDetailAddOnRepository orderDetailAddOnRepository;

    @InjectMocks
    OrderServiceImpl orderService;

    @Test
    void getTransactionDetail_forbidden_when_not_owner_and_not_staff() {
        OrderEntity order = OrderEntity.builder().orderId(1).build();
        User owner = User.builder().userId(10).build();
        order.setCustomer(owner);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        User current = User.builder().userId(20).build();
        ResponseEntity<?> res = orderService.getTransactionDetail(1, current);
        assertThat(res.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void getTransactionDetail_owner_with_payment_method_and_address_and_addons() {
        // order with customer
        User owner = User.builder().userId(30).fullName("Alice").build();
        OrderEntity order = OrderEntity.builder().orderId(2).createdAt(LocalDateTime.now()).status("COMPLETED").build();
        order.setCustomer(owner);

        // payment with method
        Payment pay = Payment.builder().method(PaymentMethod.BANKING).amount(new BigDecimal("123")).build();
        order.getPayments().add(pay);

        // address
        Address addr = Address.builder().addressLine("AddrX").build();
        order.setAddress(addr);

        // detail with product and snapshot
        Product prod = new Product();
        prod.setProductId(11);
        prod.setName("ProdName");
        prod.setImgUrl("/img.png");
        OrderDetail d = OrderDetail.builder().orderDetailId(21).product(prod).productNameSnapshot("SnapName")
                .quantity(2).unitPrice(new BigDecimal("50")).size(null).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        OrderDetailAddOn oda = OrderDetailAddOn.builder().orderDetail(d)
                .addOn(AddOn.builder().addOnId(301).name("AO").build()).unitPriceSnapshot(new BigDecimal("5")).build();

        when(orderRepository.findById(2)).thenReturn(Optional.of(order));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(21)).thenReturn(List.of(oda));

        ResponseEntity<?> res = orderService.getTransactionDetail(2, owner);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertThat(body.get("data")).isNotNull();
        List<?> data = (List<?>) body.get("data");
        assertThat(data).hasSize(1);
        Map<?, ?> detail = (Map<?, ?>) data.get(0);
        assertThat(detail.get("receiver_name")).isEqualTo("Alice");
        assertThat(detail.get("payment_name")).isEqualTo("BANKING");
        List<?> products = (List<?>) detail.get("products");
        assertThat(products).hasSize(1);
        Map<?, ?> pd = (Map<?, ?>) products.get(0);
        // snapshot preferred
        assertThat(pd.get("product_name")).isEqualTo("SnapName");
        assertThat(pd.get("product_img")).isEqualTo("/img.png");
        List<?> addOns = (List<?>) pd.get("add_ons");
        assertThat(addOns).hasSize(1);
        Map<?, ?> ao = (Map<?, ?>) addOns.get(0);
        assertThat(ao.get("name")).isEqualTo("AO");
        assertThat(ao.get("price")).isEqualTo(new BigDecimal("5"));
    }

    @Test
    void getTransactionDetail_table_deliveryName_and_payment_null_method() {
        OrderEntity order = OrderEntity.builder().orderId(3).build();
        User owner = User.builder().userId(40).build();
        order.setCustomer(owner);
        // payment with null method
        Payment pay = Payment.builder().method(null).amount(BigDecimal.ZERO).build();
        order.getPayments().add(pay);
        DiningTable table = DiningTable.builder().number(9).build();
        order.setTable(table);

        when(orderRepository.findById(3)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(3, owner);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        List<?> data = (List<?>) body.get("data");
        Map<?, ?> detail = (Map<?, ?>) data.get(0);
        // Expect payment_name to be null when payment method is null
        assertThat(detail.get("payment_name")).isNull();
        assertThat(detail.get("delivery_name")).isEqualTo("Table 9");
    }

    @Test
    void getTransactionDetail_orderDetailsNull_productsEmpty() {
        OrderEntity order = OrderEntity.builder().orderId(4).build();
        User owner = User.builder().userId(50).build();
        order.setCustomer(owner);
        order.setOrderDetails(null);
        when(orderRepository.findById(4)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(4, owner);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        List<?> data = (List<?>) body.get("data");
        Map<?, ?> detail = (Map<?, ?>) data.get(0);
        List<?> products = (List<?>) detail.get("products");
        assertThat(products).isEmpty();
    }

    @Test
    void getTransactionDetail_detailLineTotalNull_sizeAndAddonNullPriceHandled() {
        OrderEntity order = OrderEntity.builder().orderId(5).build();
        User owner = User.builder().userId(60).build();
        order.setCustomer(owner);

        Size sz = Size.builder().sizeId(7).name("XL").build();
        Product prod = new Product();
        prod.setProductId(66);
        prod.setName("X");
        OrderDetail d = OrderDetail.builder().orderDetailId(77).product(prod).productNameSnapshot(null).quantity(1)
                .lineTotal(null).unitPrice(new BigDecimal("10")).size(sz).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        OrderDetailAddOn oda = OrderDetailAddOn.builder().orderDetail(d).addOn(null).unitPriceSnapshot(null).build();

        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(77)).thenReturn(List.of(oda));

        ResponseEntity<?> res = orderService.getTransactionDetail(5, owner);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        List<?> data = (List<?>) body.get("data");
        Map<?, ?> detail = (Map<?, ?>) data.get(0);
        List<?> products = (List<?>) detail.get("products");
        Map<?, ?> pd = (Map<?, ?>) products.get(0);
        // subtotal defaulted to zero
        assertThat(pd.get("subtotal")).isEqualTo(BigDecimal.ZERO);
        // size name mapped
        assertThat(pd.get("size")).isEqualTo("XL");
        List<?> addOns = (List<?>) pd.get("add_ons");
        Map<?, ?> ao = (Map<?, ?>) addOns.get(0);
        assertThat(ao.get("name")).isNull();
        assertThat(ao.get("price")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getTransactionDetail_allowed_for_staff_role() {
        Role staffRole = Role.builder().name("STAFF").build();
        User staff = User.builder().userId(99).role(staffRole).build();

        OrderEntity order = OrderEntity.builder().orderId(6).build();
        // owner is different
        User owner = User.builder().userId(100).fullName("Owner").build();
        order.setCustomer(owner);

        when(orderRepository.findById(6)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(6, staff);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        List<?> data = (List<?>) body.get("data");
        assertThat(data).hasSize(1);
    }

    @Test
    void getTransactionDetail_receiverName_empty_when_customerNull_and_admin_allowed() {
        // order with no customer
        OrderEntity order = OrderEntity.builder().orderId(70).build();
        order.setCustomer(null);
        when(orderRepository.findById(70)).thenReturn(Optional.of(order));

        Role adminRole = Role.builder().name("ADMIN").build();
        User admin = User.builder().userId(999).role(adminRole).build();

        ResponseEntity<?> res = orderService.getTransactionDetail(70, admin);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        List<?> data = (List<?>) body.get("data");
        Map<?, ?> detail = (Map<?, ?>) data.get(0);
        assertThat(detail.get("receiver_name")).isEqualTo("");
    }

    @Test
    void getTransactionDetail_paymentEmpty_paymentNameNull_for_owner() {
        User owner = User.builder().userId(1010).fullName("Bob").build();
        OrderEntity order = OrderEntity.builder().orderId(71).build();
        order.setCustomer(owner);
        order.setPayments(new ArrayList<>());
        when(orderRepository.findById(71)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(71, owner);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> detail = (Map<?, ?>) ((List<?>) body.get("data")).get(0);
        // Expect payment_name to be null when payments list is empty
        assertThat(detail.get("payment_name")).isNull();
    }

    @Test
    void getTransactionDetail_productNull_snapshotNull_mapsNullNameAndImg() {
        User owner = User.builder().userId(2020).fullName("OwnerZ").build();
        OrderDetail d = OrderDetail.builder().orderDetailId(8000).product(null).productNameSnapshot(null).quantity(1)
                .unitPrice(new BigDecimal("10")).build();
        OrderEntity order = OrderEntity.builder().orderId(72).build();
        order.setCustomer(owner);
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        when(orderRepository.findById(72)).thenReturn(Optional.of(order));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(8000)).thenReturn(List.of());

        ResponseEntity<?> res = orderService.getTransactionDetail(72, owner);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> detail = (Map<?, ?>) ((List<?>) body.get("data")).get(0);
        List<?> products = (List<?>) detail.get("products");
        Map<?, ?> pd = (Map<?, ?>) products.get(0);
        assertThat(pd.get("product_name")).isNull();
        assertThat(pd.get("product_img")).isNull();
    }

    @Test
    void getTransactionDetail_allowed_for_employee_role() {
        Role empRole = Role.builder().name("EMPLOYEE").build();
        User emp = User.builder().userId(88).role(empRole).build();
        OrderEntity order = OrderEntity.builder().orderId(73).build();
        User owner = User.builder().userId(200).build();
        order.setCustomer(owner);
        when(orderRepository.findById(73)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(73, emp);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getTransactionDetail_notes_and_status_mapped_when_present() {
        User owner = User.builder().userId(3030).fullName("NoteOwner").build();
        OrderEntity order = OrderEntity.builder().orderId(74).note("Please hurry").status("PAID")
                .createdAt(LocalDateTime.now()).build();
        order.setCustomer(owner);
        when(orderRepository.findById(74)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(74, owner);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> detail = (Map<?, ?>) ((List<?>) body.get("data")).get(0);
        assertThat(detail.get("notes")).isEqualTo("Please hurry");
        assertThat(detail.get("status_name")).isEqualTo("PAID");
    }

    @Test
    void getTransactionDetail_sizePresentButNameNull_mapsNullSize() {
        User owner = User.builder().userId(4040).fullName("S").build();
        Size sz = Size.builder().sizeId(9).name(null).build();
        Product prod = new Product();
        prod.setProductId(9000);
        prod.setName("P");
        OrderDetail d = OrderDetail.builder().orderDetailId(900).product(prod).productNameSnapshot(null).quantity(1)
                .unitPrice(new BigDecimal("3")).size(sz).build();
        OrderEntity order = OrderEntity.builder().orderId(75).build();
        order.setCustomer(owner);
        order.setOrderDetails(new ArrayList<>(List.of(d)));
        when(orderRepository.findById(75)).thenReturn(Optional.of(order));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(900)).thenReturn(List.of());

        ResponseEntity<?> res = orderService.getTransactionDetail(75, owner);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> detail = (Map<?, ?>) ((List<?>) body.get("data")).get(0);
        Map<?, ?> pd = (Map<?, ?>) ((List<?>) detail.get("products")).get(0);
        assertThat(pd.get("size")).isNull();
    }

    @Test
    void getTransactionDetail_addOnWithNullName_priceFromSnapshot() {
        User owner = User.builder().userId(5050).fullName("AOOwner").build();
        OrderDetail d = OrderDetail.builder().orderDetailId(1000).product(null).productNameSnapshot(null).quantity(1)
                .unitPrice(new BigDecimal("4")).build();
        OrderEntity order = OrderEntity.builder().orderId(76).build();
        order.setCustomer(owner);
        order.setOrderDetails(new ArrayList<>(List.of(d)));
        OrderDetailAddOn oda = OrderDetailAddOn.builder().orderDetail(d)
                .addOn(AddOn.builder().addOnId(5001).name(null).build()).unitPriceSnapshot(new BigDecimal("7")).build();
        when(orderRepository.findById(76)).thenReturn(Optional.of(order));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(1000)).thenReturn(List.of(oda));

        ResponseEntity<?> res = orderService.getTransactionDetail(76, owner);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> detail = (Map<?, ?>) ((List<?>) body.get("data")).get(0);
        Map<?, ?> pd = (Map<?, ?>) ((List<?>) detail.get("products")).get(0);
        Map<?, ?> ao = (Map<?, ?>) ((List<?>) pd.get("add_ons")).get(0);
        assertThat(ao.get("name")).isNull();
        assertThat(ao.get("price")).isEqualTo(new BigDecimal("7"));
    }

    @Test
    void getTransactionDetail_currentNull_forbidden() {
        OrderEntity order = OrderEntity.builder().orderId(200).build();
        User owner = User.builder().userId(201).build();
        order.setCustomer(owner);
        when(orderRepository.findById(200)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(200, null);
        assertThat(res.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void getTransactionDetail_currentRoleNull_and_notOwner_forbidden() {
        User current = User.builder().userId(300).role(null).build();
        OrderEntity order = OrderEntity.builder().orderId(201).build();
        User owner = User.builder().userId(400).build();
        order.setCustomer(owner);
        when(orderRepository.findById(201)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(201, current);
        assertThat(res.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void getTransactionDetail_employee_lowercase_allowed() {
        Role empRole = Role.builder().name("employee").build();
        User emp = User.builder().userId(120).role(empRole).build();
        OrderEntity order = OrderEntity.builder().orderId(202).build();
        User owner = User.builder().userId(121).build();
        order.setCustomer(owner);
        when(orderRepository.findById(202)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(202, emp);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getTransactionDetail_paymentPresent_methodCash_mapped() {
        User owner = User.builder().userId(130).fullName("CashOwner").build();
        OrderEntity order = OrderEntity.builder().orderId(203).build();
        order.setCustomer(owner);
        Payment pay = Payment.builder().method(PaymentMethod.CASH).amount(new BigDecimal("33")).build();
        order.getPayments().add(pay);
        when(orderRepository.findById(203)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(203, owner);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> det = (Map<?, ?>) ((List<?>) body.get("data")).get(0);
        assertThat(det.get("payment_name")).isEqualTo("CASH");
    }

    @Test
    void getTransactionDetail_paymentFee_defaultZero_and_paymentName_present() {
        User owner = User.builder().userId(140).fullName("FeeOwner").build();
        OrderEntity order = OrderEntity.builder().orderId(204).build();
        order.setCustomer(owner);
        Payment pay = Payment.builder().method(PaymentMethod.BANKING).amount(new BigDecimal("100")).build();
        order.getPayments().add(pay);
        when(orderRepository.findById(204)).thenReturn(Optional.of(order));

        ResponseEntity<?> res = orderService.getTransactionDetail(204, owner);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> det = (Map<?, ?>) ((List<?>) body.get("data")).get(0);
        assertThat(det.get("payment_name")).isEqualTo("BANKING");
        assertThat(det.get("payment_fee")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getTransactionDetail_productWithNullImg_mapsNullProductImg() {
        User owner = User.builder().userId(150).fullName("ImgOwner").build();
        Product prod = new Product();
        prod.setProductId(1600);
        prod.setName("HasNoImg");
        prod.setImgUrl(null);
        OrderDetail d = OrderDetail.builder().orderDetailId(160).product(prod).productNameSnapshot(null).quantity(1)
                .unitPrice(new BigDecimal("12")).build();
        OrderEntity order = OrderEntity.builder().orderId(205).build();
        order.setCustomer(owner);
        order.setOrderDetails(new ArrayList<>(List.of(d)));
        when(orderRepository.findById(205)).thenReturn(Optional.of(order));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(160)).thenReturn(List.of());

        ResponseEntity<?> res = orderService.getTransactionDetail(205, owner);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> det = (Map<?, ?>) ((List<?>) body.get("data")).get(0);
        Map<?, ?> pd = (Map<?, ?>) ((List<?>) det.get("products")).get(0);
        assertThat(pd.get("product_img")).isNull();
        assertThat(pd.get("product_name")).isEqualTo("HasNoImg");
    }

    @Test
    void getTransactionDetail_multipleProducts_and_addons_mapped() {
        User owner = User.builder().userId(160).fullName("Multi").build();
        Product p1 = new Product();
        p1.setProductId(1001);
        p1.setName("P1");
        p1.setImgUrl("/p1.png");
        Product p2 = new Product();
        p2.setProductId(1002);
        p2.setName("P2");
        p2.setImgUrl("/p2.png");
        OrderDetail d1 = OrderDetail.builder().orderDetailId(1001).product(p1).productNameSnapshot(null).quantity(1)
                .unitPrice(new BigDecimal("5")).build();
        OrderDetail d2 = OrderDetail.builder().orderDetailId(1002).product(p2).productNameSnapshot(null).quantity(2)
                .unitPrice(new BigDecimal("7")).build();
        OrderEntity order = OrderEntity.builder().orderId(206).build();
        order.setCustomer(owner);
        order.setOrderDetails(new ArrayList<>(List.of(d1, d2)));

        OrderDetailAddOn ao1 = OrderDetailAddOn.builder().orderDetail(d1)
                .addOn(AddOn.builder().addOnId(9001).name("A1").build()).unitPriceSnapshot(new BigDecimal("2")).build();
        OrderDetailAddOn ao2 = OrderDetailAddOn.builder().orderDetail(d2)
                .addOn(AddOn.builder().addOnId(9002).name("A2").build()).unitPriceSnapshot(new BigDecimal("3")).build();

        when(orderRepository.findById(206)).thenReturn(Optional.of(order));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(1001)).thenReturn(List.of(ao1));
        when(orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(1002)).thenReturn(List.of(ao2));

        ResponseEntity<?> res = orderService.getTransactionDetail(206, owner);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> det = (Map<?, ?>) ((List<?>) body.get("data")).get(0);
        List<?> products = (List<?>) det.get("products");
        assertThat(products).hasSize(2);
    }
}
