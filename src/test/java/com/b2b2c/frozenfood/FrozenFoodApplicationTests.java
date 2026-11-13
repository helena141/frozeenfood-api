package com.b2b2c.frozenfood;

import com.lezzat.b2b2c.frozenfood.FrozenFoodApplication;
import com.lezzat.b2b2c.frozenfood.dto.request.PaymentCallbackRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductCatalogResponse;
import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.entity.Payment;
import com.lezzat.b2b2c.frozenfood.entity.Product;
import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import com.lezzat.b2b2c.frozenfood.entity.TieringPrice;
import com.lezzat.b2b2c.frozenfood.repository.ExpiryAlertRepository;
import com.lezzat.b2b2c.frozenfood.repository.OrderRepository;
import com.lezzat.b2b2c.frozenfood.repository.PaymentRepository;
import com.lezzat.b2b2c.frozenfood.repository.ProductBatchRepository;
import com.lezzat.b2b2c.frozenfood.repository.ProductRepository;
import com.lezzat.b2b2c.frozenfood.repository.TieringPriceRepository;
import com.lezzat.b2b2c.frozenfood.service.PaymentService;
import com.lezzat.b2b2c.frozenfood.service.ProductService;
import com.lezzat.b2b2c.frozenfood.service.TieringPriceService;
import com.lezzat.b2b2c.frozenfood.service.core.ExpiryAlertScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = FrozenFoodApplication.class)
class FrozenFoodApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void verifyClearanceAndPaymentSync() {
        ProductRepository productRepo = mock(ProductRepository.class);
        TieringPriceRepository tieringRepo = mock(TieringPriceRepository.class);
        Product product = Product.builder()
                .idProduk("NGT-001")
                .namaProduk("Nugget Ayam")
                .hargaEceran(new BigDecimal("40000.00"))
                .build();
        ProductBatch clearanceBatch = ProductBatch.builder()
                .idProdBatch("B-NGT001-CLEAR")
                .product(product)
                .stokOnline(10)
                .tanggalExp(LocalDate.now().plusDays(5))
                .build();
        product.setProductBatches(List.of(clearanceBatch));

        when(productRepo.findById("NGT-001")).thenReturn(Optional.of(product));
        when(tieringRepo.findByProduct_IdProdukOrderByMinQtyAsc("NGT-001")).thenReturn(List.of(
                TieringPrice.builder().idTiering(UUID.randomUUID()).minQty(10).maxQty(50).hargaPerUnit(new BigDecimal("35000.00")).build()
        ));

        ProductBatchRepository batchRepo = mock(ProductBatchRepository.class);
        TieringPriceService tieringPriceService = new TieringPriceService(tieringRepo, batchRepo);
        ProductService productService = new ProductService(productRepo, tieringRepo, tieringPriceService);
        ProductCatalogResponse catalog = productService.getProductById("NGT-001");
        assertTrue(catalog.getIsClearance());
        assertEquals(10, catalog.getStokClearance());
        assertEquals(new BigDecimal("20000.00"), catalog.getHargaClearance());
        assertEquals(1, catalog.getTieringPrices().size());

        ExpiryAlertRepository alertRepo = mock(ExpiryAlertRepository.class);
        when(batchRepo.findBatchesExpiringBefore(any())).thenReturn(List.of(clearanceBatch));
        when(alertRepo.findFirstByProductBatchAndIsResolvedFalseOrderByCreatedAtDesc(clearanceBatch)).thenReturn(Optional.empty());

        new ExpiryAlertScheduler(batchRepo, alertRepo).scanAndGenerateExpiryAlerts();
        assertEquals(new BigDecimal("50.00"), clearanceBatch.getDiskon());

        PaymentRepository paymentRepo = mock(PaymentRepository.class);
        OrderRepository orderRepo = mock(OrderRepository.class);
        Order order = Order.builder().idOrder(UUID.randomUUID()).status("MENUNGGU_PEMBAYARAN").build();
        Payment payment = Payment.builder().idPayment(UUID.randomUUID()).order(order).gatewayRef("MOCK-1").status("PENDING").build();
        when(paymentRepo.findByGatewayRef("MOCK-1")).thenReturn(Optional.of(payment));

        new PaymentService(paymentRepo, orderRepo).handleCallback(new PaymentCallbackRequest("MOCK-1", "LUNAS", BigDecimal.ZERO));
        assertEquals("DIBAYAR", order.getStatus());
    }
}