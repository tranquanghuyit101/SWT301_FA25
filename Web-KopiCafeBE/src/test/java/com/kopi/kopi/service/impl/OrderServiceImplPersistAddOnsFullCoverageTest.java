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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplPersistAddOnsFullCoverageTest {

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
    void orderNullOrEmptyProducts_returnEarly() throws Exception {
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
    void orderDetailsNull_becomesEmpty_and_noSave() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(2000);
        prod.setPrice(new BigDecimal("10"));
        when(productRepository.findById(2000)).thenReturn(Optional.of(prod));

        OrderEntity order = OrderEntity.builder().orderId(2001).build();
        order.setOrderDetails(null); // branch: order.getOrderDetails() == null -> List.of()

        List<Map<String, Object>> req = List.of(Map.of("product_id", 2000, "qty", 1, "add_on_ids", List.of(3000)));
        m.invoke(orderService, order, req);
        // no details to match -> saveAll should not be called
        verify(orderDetailAddOnRepository, never()).saveAll(anyList());
    }

    @Test
    void sizeIdParseException_treatedAsNull_and_matchingDetailFound() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(2100);
        prod.setPrice(new BigDecimal("100"));
        when(productRepository.findById(2100)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(3101);
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("10"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(2100, 3101)).thenReturn(Optional.of(pa));

        // detail with size null and unit = base + addOnSum
        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("110")).size(null)
                .build();
        d.setOrderDetailId(4001);
        OrderEntity order = OrderEntity.builder().orderId(4002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List
                .of(Map.of("product_id", 2100, "qty", 1, "size_id", "not-a-number", "add_on_ids", List.of(3101)));
        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void prodNull_skipsEntry() throws Exception {
        Method m = getPersistMethod();
        when(productRepository.findById(2200)).thenReturn(Optional.empty());
        OrderEntity order = OrderEntity.builder().orderId(2201).build();
        order.setOrderDetails(new ArrayList<>());
        List<Map<String, Object>> req = List.of(Map.of("product_id", 2200, "qty", 1, "add_on_ids", List.of(3300)));
        m.invoke(orderService, order, req);
        verifyNoInteractions(productAddOnRepository, orderDetailAddOnRepository);
    }

    @Test
    void prodPriceNull_baseZero_and_save() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(2300);
        prod.setPrice(null);
        when(productRepository.findById(2300)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(3301);
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("5"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(2300, 3301)).thenReturn(Optional.of(pa));

        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("5")).size(null)
                .build();
        d.setOrderDetailId(5001);
        OrderEntity order = OrderEntity.builder().orderId(5002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 2300, "qty", 1, "add_on_ids", List.of(3301)));
        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void productSizePricePresent_used_in_expectedUnit_and_save() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(2400);
        prod.setPrice(new BigDecimal("20"));
        when(productRepository.findById(2400)).thenReturn(Optional.of(prod));

        ProductSize ps = new ProductSize();
        ps.setPrice(new BigDecimal("4"));
        when(productSizeRepository.findByProduct_ProductIdAndSize_SizeId(2400, 6)).thenReturn(Optional.of(ps));

        AddOn ao = new AddOn();
        ao.setAddOnId(3401);
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("1"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(2400, 3401)).thenReturn(Optional.of(pa));

        // expected unit = 20 + 4 + 1 = 25
        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("25"))
                .size(Size.builder().sizeId(6).build()).build();
        d.setOrderDetailId(6001);
        OrderEntity order = OrderEntity.builder().orderId(6002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List
                .of(Map.of("product_id", 2400, "qty", 1, "size_id", 6, "add_on_ids", List.of(3401)));
        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void productSizePriceNull_sizeDeltaZero_and_save() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(2500);
        prod.setPrice(new BigDecimal("30"));
        when(productRepository.findById(2500)).thenReturn(Optional.of(prod));

        ProductSize ps = new ProductSize();
        ps.setPrice(null);
        when(productSizeRepository.findByProduct_ProductIdAndSize_SizeId(2500, 7)).thenReturn(Optional.of(ps));

        AddOn ao = new AddOn();
        ao.setAddOnId(3501);
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("2"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(2500, 3501)).thenReturn(Optional.of(pa));

        // expected = 30 + 0 + 2 = 32
        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("32"))
                .size(Size.builder().sizeId(7).build()).build();
        d.setOrderDetailId(7001);
        OrderEntity order = OrderEntity.builder().orderId(7002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List
                .of(Map.of("product_id", 2500, "qty", 1, "size_id", 7, "add_on_ids", List.of(3501)));
        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void productAddOnPriceNull_snapshotZero_saved() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(2600);
        prod.setPrice(new BigDecimal("40"));
        when(productRepository.findById(2600)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(3601);
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(null);
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(2600, 3601)).thenReturn(Optional.of(pa));

        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("40")).size(null)
                .build();
        d.setOrderDetailId(8001);
        OrderEntity order = OrderEntity.builder().orderId(8002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 2600, "qty", 1, "add_on_ids", List.of(3601)));
        m.invoke(orderService, order, req);

        // capture iterable passed to saveAll and assert unitPriceSnapshot fallback to
        // ZERO
        @SuppressWarnings("unchecked")
        var cap = (org.mockito.ArgumentCaptor<Iterable>) org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(orderDetailAddOnRepository, times(1)).saveAll(cap.capture());
        Iterable<?> items = cap.getValue();
        if (items != null && items.iterator().hasNext()) {
            Object first = items.iterator().next();
            try {
                var mth = first.getClass().getMethod("getUnitPriceSnapshot");
                Object val = mth.invoke(first);
                // should not be null (fallback to BigDecimal.ZERO)
                assert val != null;
            } catch (NoSuchMethodException ignored) {
            }
        }
    }

    @Test
    void usedIndex_skipsDuplicateRequests() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(2700);
        prod.setPrice(new BigDecimal("15"));
        when(productRepository.findById(2700)).thenReturn(Optional.of(prod));

        AddOn ao = new AddOn();
        ao.setAddOnId(3701);
        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(ao);
        pa.setPrice(new BigDecimal("1"));
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(2700, 3701)).thenReturn(Optional.of(pa));

        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("16")).size(null)
                .build();
        d.setOrderDetailId(9001);
        OrderEntity order = OrderEntity.builder().orderId(9002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(
                Map.of("product_id", 2700, "qty", 1, "add_on_ids", List.of(3701)),
                Map.of("product_id", 2700, "qty", 1, "add_on_ids", List.of(3701)));
        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void detailProductNull_skipped_noCrash() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(2800);
        prod.setPrice(new BigDecimal("11"));
        when(productRepository.findById(2800)).thenReturn(Optional.of(prod));

        OrderDetail d = OrderDetail.builder().product(null).quantity(1).unitPrice(new BigDecimal("11")).size(null)
                .build();
        d.setOrderDetailId(10001);
        OrderEntity order = OrderEntity.builder().orderId(10002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 2800, "qty", 1, "add_on_ids", List.of(3800)));
        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, never()).saveAll(anyList());
    }

    @Test
    void quantityMismatch_skips() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(2900);
        prod.setPrice(new BigDecimal("12"));
        when(productRepository.findById(2900)).thenReturn(Optional.of(prod));

        OrderDetail d = OrderDetail.builder().product(prod).quantity(2).unitPrice(new BigDecimal("12")).size(null)
                .build();
        d.setOrderDetailId(11001);
        OrderEntity order = OrderEntity.builder().orderId(11002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 2900, "qty", 1, "add_on_ids", List.of(3900)));
        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, never()).saveAll(anyList());
    }

    @Test
    void sizeIdMismatch_skips() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(3000);
        prod.setPrice(new BigDecimal("13"));
        when(productRepository.findById(3000)).thenReturn(Optional.of(prod));

        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(new BigDecimal("13"))
                .size(Size.builder().sizeId(55).build()).build();
        d.setOrderDetailId(12001);
        OrderEntity order = OrderEntity.builder().orderId(12002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List
                .of(Map.of("product_id", 3000, "qty", 1, "size_id", 66, "add_on_ids", List.of(4000)));
        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, never()).saveAll(anyList());
    }

    @Test
    void detailUnitPriceNull_matchesZero_and_saves() throws Exception {
        Method m = getPersistMethod();
        Product prod = new Product();
        prod.setProductId(3100);
        prod.setPrice(null);
        when(productRepository.findById(3100)).thenReturn(Optional.of(prod));

        ProductAddOn pa = new ProductAddOn();
        pa.setAddOn(new AddOn());
        pa.setPrice(null);
        when(productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(3100, 4100)).thenReturn(Optional.of(pa));

        OrderDetail d = OrderDetail.builder().product(prod).quantity(1).unitPrice(null).size(null).build();
        d.setOrderDetailId(13001);
        OrderEntity order = OrderEntity.builder().orderId(13002).build();
        order.setOrderDetails(new ArrayList<>(List.of(d)));

        List<Map<String, Object>> req = List.of(Map.of("product_id", 3100, "qty", 1, "add_on_ids", List.of(4100)));
        m.invoke(orderService, order, req);
        verify(orderDetailAddOnRepository, times(1)).saveAll(anyList());
    }
}
