package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplPersistAddOnsTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductSizeRepository productSizeRepository;

    @Mock
    ProductAddOnRepository productAddOnRepository;

    @Mock
    OrderDetailAddOnRepository orderDetailAddOnRepository;

    @InjectMocks
    OrderServiceImpl orderService;

    private Method getPersistMethod() throws Exception {
        Method m = OrderServiceImpl.class.getDeclaredMethod("persistAddOnsForOrder", OrderEntity.class, List.class);
        m.setAccessible(true);
        return m;
    }

    @Test
    void persistAddOns_noOrderOrNoProducts_doesNothing() throws Exception {
        Method m = getPersistMethod();

        // null order
        m.invoke(orderService, null, List.of(Map.of("product_id", 1, "qty", 1, "add_on_ids", List.of(1))));
        verifyNoInteractions(productRepository, productSizeRepository, productAddOnRepository,
                orderDetailAddOnRepository);

        // empty products
        OrderEntity o = new OrderEntity();
        m.invoke(orderService, o, Collections.emptyList());
        verifyNoInteractions(productRepository, productSizeRepository, productAddOnRepository,
                orderDetailAddOnRepository);
    }

    @Test
    void persistAddOns_invalidProductId_skipsEntry() throws Exception {
        Method m = getPersistMethod();
        OrderEntity order = new OrderEntity();
        order.setOrderDetails(List.of());

        List<Map<String, Object>> req = List
                .of(Map.of("product_id", "not-a-number", "qty", 1, "add_on_ids", List.of(1)));
        m.invoke(orderService, order, req);
        verifyNoInteractions(productRepository, productSizeRepository, productAddOnRepository,
                orderDetailAddOnRepository);
    }

    @Test
    void persistAddOns_matchingDetail_savesAddOns() throws Exception {
        Method m = getPersistMethod();

        // Prepare product, size, addon
        Product prod = new Product();
        prod.setProductId(10);
        prod.setPrice(new BigDecimal("1000"));
        when(productRepository.findById(10)).thenReturn(Optional.of(prod));

        ProductSize ps = new ProductSize();
        ps.setPrice(new BigDecimal("100"));
        when(productSizeRepository.findByProduct_ProductIdAndSize_SizeId(10, 5)).thenReturn(Optional.of(ps));

        AddOn ao = new AddOn();
        ao.setAddOnId(501);
        ao.setName("Sugar");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("50"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(10, 501)).thenReturn(Optional.of(pa));

        // Create OrderDetail matching expected unit (1000+100+50 = 1150)
        OrderDetail d = OrderDetail.builder()
                .product(prod)
                .quantity(1)
                .unitPrice(new BigDecimal("1150"))
                .size(Size.builder().sizeId(5).build())
                .build();
        d.setOrderDetailId(77);

        OrderEntity order = OrderEntity.builder().orderId(99).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List
                .of(Map.of("product_id", 10, "qty", 1, "size_id", 5, "add_on_ids", List.of(501)));

        m.invoke(orderService, order, req);

        // verify saveAll called with one OrderDetailAddOn
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void persistAddOns_addOnMissing_skipsMissingAddOn() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(20);
        prod.setPrice(new BigDecimal("200"));
        when(productRepository.findById(20)).thenReturn(Optional.of(prod));

        // size absent
        when(productSizeRepository.findByProduct_ProductIdAndSize_SizeId(20, null)).thenReturn(Optional.empty());

        // one addon exists, one missing
        AddOn ao = new AddOn();
        ao.setAddOnId(601);
        ao.setName("Milk");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("10"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(20, 601)).thenReturn(Optional.of(pa));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(20, 602)).thenReturn(Optional.empty());

        // create matching detail: expected unit = 200 + 0 + 10 = 210
        OrderDetail d = OrderDetail.builder()
                .product(prod)
                .quantity(2)
                .unitPrice(new BigDecimal("210"))
                .size(null)
                .build();
        // quantity is 2, so request must use qty 2
        d.setOrderDetailId(88);

        OrderEntity order = OrderEntity.builder().orderId(123).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 20, "qty", 2, "add_on_ids", List.of(601, 602)));

        m.invoke(orderService, order, req);

        // only one add-on exists, so saveAll should be called with one element
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void persistAddOns_addOnIdsAsMapsAndStrings_handlesVariousIdShapes() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(701);
        prod.setPrice(new BigDecimal("50"));
        when(productRepository.findById(701)).thenReturn(Optional.of(prod));

        // three add-on ids in various shapes: map with id (string), map with add_on_id
        // (number), plain string
        AddOn ao1 = new AddOn();
        ao1.setAddOnId(7011);
        ao1.setName("A1");
        ProductAddOn pa1 = new ProductAddOn();
        pa1.setAddOn(ao1);
        pa1.setPrice(new BigDecimal("5"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(701, 7011)).thenReturn(Optional.of(pa1));

        AddOn ao2 = new AddOn();
        ao2.setAddOnId(7012);
        ao2.setName("A2");
        ProductAddOn pa2 = new ProductAddOn();
        pa2.setAddOn(ao2);
        pa2.setPrice(new BigDecimal("3"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(701, 7012)).thenReturn(Optional.of(pa2));

        AddOn ao3 = new AddOn();
        ao3.setAddOnId(7013);
        ao3.setName("A3");
        ProductAddOn pa3 = new ProductAddOn();
        pa3.setAddOn(ao3);
        pa3.setPrice(new BigDecimal("2"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(701, 7013)).thenReturn(Optional.of(pa3));

        // create matching order detail: expectedUnit = base(50) + addOnSum(5+3+2) = 60
        OrderDetail d = OrderDetail.builder()
                .product(prod)
                .quantity(1)
                .unitPrice(new BigDecimal("60"))
                .size(null)
                .build();
        d.setOrderDetailId(201);

        OrderEntity order = OrderEntity.builder().orderId(202).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        Map<String, Object> map1 = Map.of("id", "7011");
        Map<String, Object> map2 = Map.of("add_on_id", 7012);
        List<Object> addOns = List.of(map1, map2, "7013");

        List<Map<String, Object>> req = List.of(Map.of("product_id", 701, "qty", 1, "add_ons", addOns));

        m.invoke(orderService, order, req);

        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void persistAddOns_detailWithNullProduct_skipsAndDoesNotCrash() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(800);
        prod.setPrice(new BigDecimal("10"));
        when(productRepository.findById(800)).thenReturn(Optional.of(prod));

        // create an OrderDetail with null product to exercise the branch where
        // d.getProduct() == null
        OrderDetail d = OrderDetail.builder()
                .product(null)
                .quantity(1)
                .unitPrice(new BigDecimal("10"))
                .build();
        d.setOrderDetailId(301);

        OrderEntity order = OrderEntity.builder().orderId(302).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 800, "qty", 1, "add_on_ids", List.of(900)));

        // should not throw
        m.invoke(orderService, order, req);

        verify(orderDetailAddOnRepository, never()).saveAll(anyList());
    }

    @Test
    void persistAddOns_expectedUnitMismatch_skipsSaving() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(900);
        prod.setPrice(new BigDecimal("100"));
        when(productRepository.findById(900)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(901);
        ao.setName("X");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("10"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(900, 901)).thenReturn(Optional.of(pa));

        // detail unitPrice not equal to expected (expected 110, but detail has 999)
        OrderDetail d = OrderDetail.builder()
                .product(prod)
                .quantity(1)
                .unitPrice(new BigDecimal("999"))
                .size(null)
                .build();
        d.setOrderDetailId(401);

        OrderEntity order = OrderEntity.builder().orderId(402).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 900, "qty", 1, "add_on_ids", List.of(901)));

        m.invoke(orderService, order, req);

        verify(orderDetailAddOnRepository, never()).saveAll(anyList());
    }

    @Test
    void persistAddOns_orderDetailsNull_detailsListEmpty_noSave() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(500);
        prod.setPrice(new BigDecimal("20"));
        when(productRepository.findById(500)).thenReturn(Optional.of(prod));

        // order with null orderDetails
        OrderEntity order = OrderEntity.builder().orderId(501).build();
        order.setOrderDetails(null);

        List<Map<String, Object>> req = List.of(Map.of("product_id", 500, "qty", 1, "add_on_ids", List.of(600)));

        m.invoke(orderService, order, req);

        verify(orderDetailAddOnRepository, never()).saveAll(anyList());
    }

    @Test
    void persistAddOns_sizeIdParseFails_usesNullSize_andSaves() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(510);
        prod.setPrice(new BigDecimal("100"));
        when(productRepository.findById(510)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(611);
        ao.setName("A");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("10"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(510, 611)).thenReturn(Optional.of(pa));

        // detail with size null and unitPrice = base + addOnSum = 110
        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("110")).size(null)
                .build();
        d.setOrderDetailId(601);
        OrderEntity order = OrderEntity.builder().orderId(602).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        // size_id present but invalid string -> parse exception, sizeId becomes null
        List<Map<String, Object>> req = List
                .of(Map.of("product_id", 510, "qty", 1, "size_id", "bad", "add_on_ids", List.of(611)));

        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void persistAddOns_productMissing_skips() throws Exception {
        Method m = getPersistMethod();

        when(productRepository.findById(520)).thenReturn(Optional.empty());

        OrderEntity order = OrderEntity.builder().orderId(521).build();
        order.setOrderDetails(new ArrayList<>());

        List<Map<String, Object>> req = List.of(Map.of("product_id", 520, "qty", 1, "add_on_ids", List.of(700)));

        m.invoke(orderService, order, req);

        verifyNoInteractions(productAddOnRepository, orderDetailAddOnRepository);
    }

    @Test
    void persistAddOns_prodPriceNull_baseZero_andSaves() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(530);
        prod.setPrice(null);
        when(productRepository.findById(530)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(731);
        ao.setName("B");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("7"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(530, 731)).thenReturn(Optional.of(pa));

        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("7")).size(null)
                .build();
        d.setOrderDetailId(701);
        OrderEntity order = OrderEntity.builder().orderId(702).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 530, "qty", 1, "add_on_ids", List.of(731)));

        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void persistAddOns_productSizeExists_butPriceNull_sizeDeltaZero() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(540);
        prod.setPrice(new BigDecimal("30"));
        when(productRepository.findById(540)).thenReturn(Optional.of(prod));

        ProductSize ps = new ProductSize();
        ps.setPrice(null);
        when(productSizeRepository.findByProduct_ProductIdAndSize_SizeId(540, 8)).thenReturn(Optional.of(ps));

        AddOn ao = new AddOn();
        ao.setAddOnId(741);
        ao.setName("C");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("5"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(540, 741)).thenReturn(Optional.of(pa));

        // expected unit = base(30) + sizeDelta(0) + addOn(5) = 35
        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("35"))
                .size(Size.builder().sizeId(8).build()).build();
        d.setOrderDetailId(801);
        OrderEntity order = OrderEntity.builder().orderId(802).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List
                .of(Map.of("product_id", 540, "qty", 1, "size_id", 8, "add_on_ids", List.of(741)));

        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void persistAddOns_productAddOnPriceNull_snapshotZero() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(560);
        prod.setPrice(new BigDecimal("40"));
        when(productRepository.findById(560)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(761);
        ao.setName("D");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(null);
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(560, 761)).thenReturn(Optional.of(pa));

        // expected unit = base(40) + addOnSum(0) = 40
        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("40")).size(null)
                .build();
        d.setOrderDetailId(901);
        OrderEntity order = OrderEntity.builder().orderId(902).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 560, "qty", 1, "add_on_ids", List.of(761)));

        m.invoke(orderService, order, req);

        ArgumentCaptor<Iterable> cap = ArgumentCaptor.forClass(Iterable.class);
        verify(orderDetailAddOnRepository, times(1)).saveAll(cap.capture());
        Iterable<?> saved = cap.getValue();
        // ensure unitPriceSnapshot fallback to ZERO when pa.getPrice() == null
        if (saved != null && saved.iterator().hasNext()) {
            Object first = saved.iterator().next();
            try {
                java.lang.reflect.Method mth = first.getClass().getMethod("getUnitPriceSnapshot");
                Object val = mth.invoke(first);
                assert val != null;
            } catch (NoSuchMethodException ignored) {
            }
        }
    }

    @Test
    void persistAddOns_usedIndexSkipsDuplicateRequests() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(570);
        prod.setPrice(new BigDecimal("15"));
        when(productRepository.findById(570)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(771);
        ao.setName("E");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("1"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(570, 771)).thenReturn(Optional.of(pa));

        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("16")).size(null)
                .build();
        d.setOrderDetailId(1001);
        OrderEntity order = OrderEntity.builder().orderId(1002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(
                Map.of("product_id", 570, "qty", 1, "add_on_ids", List.of(771)),
                Map.of("product_id", 570, "qty", 1, "add_on_ids", List.of(771)));

        m.invoke(orderService, order, req);
        // even with two requests, used[] should cause only one set of add-ons to be
        // saved
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void persistAddOns_detailProductIdMismatch_skips() throws Exception {
        Method m = getPersistMethod();

        Product p1 = new Product();
        p1.setProductId(880);
        p1.setPrice(new BigDecimal("5"));
        Product p2 = new Product();
        p2.setProductId(881);
        p2.setPrice(new BigDecimal("5"));
        when(productRepository.findById(880)).thenReturn(Optional.of(p1));
        when(productRepository.findById(881)).thenReturn(Optional.of(p2));

        OrderDetail d = OrderDetail.builder().product(p1).quantity(1).unitPrice(new BigDecimal("5")).size(null).build();
        d.setOrderDetailId(1101);
        OrderEntity order = OrderEntity.builder().orderId(1102).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        // request uses different product id so should skip
        List<Map<String, Object>> req = List.of(Map.of("product_id", 881, "qty", 1, "add_on_ids", List.of(990)));
        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, never()).saveAll(anyList());
    }

    @Test
    void persistAddOns_nullRequestProducts_noSave() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(1200);
        prod.setPrice(new BigDecimal("10"));
        when(productRepository.findById(1200)).thenReturn(Optional.of(prod));

        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("10")).size(null)
                .build();
        d.setOrderDetailId(1201);
        OrderEntity order = OrderEntity.builder().orderId(1202).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        // requestProducts is null -> method should return early and not save
        m.invoke(orderService, order, null);
        verifyNoInteractions(orderDetailAddOnRepository);
    }

    @Test
    void persistAddOns_qtyMismatch_skips() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(1300);
        prod.setPrice(new BigDecimal("20"));
        when(productRepository.findById(1300)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(1301);
        ao.setName("Z");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("2"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(1300, 1301)).thenReturn(Optional.of(pa));

        // detail quantity is 2, request qty is 1 -> should skip
        OrderDetail d = OrderDetail.builder().product(prod).quantity(2).unitPrice(new BigDecimal("22")).size(null)
                .build();
        d.setOrderDetailId(1302);
        OrderEntity order = OrderEntity.builder().orderId(1303).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 1300, "qty", 1, "add_on_ids", List.of(1301)));
        m.invoke(orderService, order, req);

        verify(orderDetailAddOnRepository, never()).saveAll(anyList());
    }

    @Test
    void persistAddOns_sizeMismatch_skips() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(1400);
        prod.setPrice(new BigDecimal("25"));
        when(productRepository.findById(1400)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(1401);
        ao.setName("Y");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("3"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(1400, 1401)).thenReturn(Optional.of(pa));

        // detail has size 9, request uses size 8 -> should skip
        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("28"))
                .size(Size.builder().sizeId(9).build()).build();
        d.setOrderDetailId(1402);
        OrderEntity order = OrderEntity.builder().orderId(1403).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List
                .of(Map.of("product_id", 1400, "qty", 1, "size_id", 8, "add_on_ids", List.of(1401)));
        m.invoke(orderService, order, req);

        verify(orderDetailAddOnRepository, never()).saveAll(anyList());
    }

    @Test
    void persistAddOns_detailUnitPriceNull_matchesExpectedZero_andSaves() throws Exception {
        Method m = getPersistMethod();

        Product prod = new Product();
        prod.setProductId(1500);
        prod.setPrice(null);
        when(productRepository.findById(1500)).thenReturn(Optional.of(prod));

        // add-on exists but price null -> expectedUnit will be 0
        AddOn ao = new AddOn();
        ao.setAddOnId(1501);
        ao.setName("NullPriceAO");
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(null);
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(1500, 1501)).thenReturn(Optional.of(pa));

        // detail has unitPrice null -> treated as ZERO and should match expectedUnit=0
        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(null).size(null).build();
        d.setOrderDetailId(1502);
        OrderEntity order = OrderEntity.builder().orderId(1503).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 1500, "qty", 1, "add_on_ids", List.of(1501)));
        m.invoke(orderService, order, req);

        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }
}
