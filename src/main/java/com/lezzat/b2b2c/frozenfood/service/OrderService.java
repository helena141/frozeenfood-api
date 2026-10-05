package com.lezzat.b2b2c.frozenfood.service;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lezzat.b2b2c.frozenfood.dto.request.OrderRequest;
import com.lezzat.b2b2c.frozenfood.entity.Customer;
import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.entity.OrderItem;
import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import com.lezzat.b2b2c.frozenfood.repository.CustomerRepository;
import com.lezzat.b2b2c.frozenfood.repository.OrderRepository;
import com.lezzat.b2b2c.frozenfood.repository.ProductBatchRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductBatchRepository productBatchRepository;
    private final TieringPriceService tieringPriceService;

    // Dependency Injection standar Enterprise
    public OrderService(OrderRepository orderRepository, 
                        CustomerRepository customerRepository, 
                        ProductBatchRepository productBatchRepository,
                        TieringPriceService tieringPriceService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productBatchRepository = productBatchRepository;
        this.tieringPriceService = tieringPriceService;
    }

    /**
     * @Transactional menjamin Doktrin ACID. Jika stok gagal dipotong, 
     * atau error di tengah jalan, seluruh transaksi (termasuk Order yang udah dibuat)
     * akan di-rollback ke state awal.
     */
    @Transactional
    public Order createOrder(OrderRequest request) {
        
        // 1. INTEROGASI JWT (Otoritas absolut, anti-manipulasi Frontend)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        // Ekstrak Role (B2B / B2C) dari Token JWT
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""); // Hapus prefix ROLE_ 
        
        // Validasi Kriminalitas Token
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kriminal: Token valid tapi user tidak ada di Database!"));

        // 2. KONSTRUKSI STRUK (Order Header)
        Order order = Order.builder()
                .customer(customer)
                .tipeOrder(role)
                .status("MENUNGGU_PEMBAYARAN")
                .ongkir(new BigDecimal("15000.00")) // Bisa diganti logic dinamis nanti
                .items(new ArrayList<>())
                .totalTagihan(BigDecimal.ZERO)
                .build();

        BigDecimal grandTotal = BigDecimal.ZERO;

        // 3. MESIN ITERASI BARANG & PEMOTONGAN STOK
        for (OrderRequest.ItemRequest itemReq : request.getItems()) {
            
            // Tarik data fisik barang dari gudang
            ProductBatch batch = productBatchRepository.findById(itemReq.getBatchId())
                    .orElseThrow(() -> new RuntimeException("Data Corrupt: Batch ID " + itemReq.getBatchId() + " tidak ditemukan!"));
            
            int quantity = itemReq.getQuantity();

            // Proteksi Over-Selling
            if (batch.getStokOnline() < quantity) {
                throw new RuntimeException("Stok tidak mencukupi untuk produk: " + batch.getProduct().getNamaProduk() + ". Sisa stok: " + batch.getStokOnline());
            }

            // Pemotongan Stok Aktif (Akan otomatis di-commit oleh @Transactional di akhir)
            batch.setStokOnline(batch.getStokOnline() - quantity);

            // 4. ALGORITMA HARGA (Cek harga override dari batch, jika null pakai harga master produk)
            BigDecimal hargaDasar = batch.getHargaOverride() != null ? 
                                    batch.getHargaOverride() : 
                                    batch.getProduct().getHargaEceran();
            
            // Hantam ke kalkulator B2B (Membanting harga jika kuantitas memenuhi syarat Tier)
            BigDecimal hargaFinal = tieringPriceService.calculateFinalPrice(role, quantity, hargaDasar);
            BigDecimal subtotal = hargaFinal.multiply(new BigDecimal(quantity));
            
            grandTotal = grandTotal.add(subtotal);

            // 5. RAKIT NOTA DETAIL (OrderItem)
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productBatch(batch)
                    .quantity(quantity)
                    .hargaSatuanSnapshot(hargaFinal) // Sejarah harga terkunci absolut
                    .subtotalItem(subtotal)
                    .build();
            
            order.getItems().add(orderItem);
        }

        // 6. FINALISASI TRANSAKSI
        order.setTotalTagihan(grandTotal.add(order.getOngkir()));
        
        // Cascade.ALL pada relasi OneToMany akan otomatis menyimpan Order beserta seluruh OrderItem-nya
        return orderRepository.save(order);
    }
}