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
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kriminal: Token valid tapi user tidak ada di Database!"));

        Order order = Order.builder()
                .customer(customer)
                .tipeOrder(role)
                .status("MENUNGGU_PEMBAYARAN")
                .ongkir(new BigDecimal("15000.00")) 
                .items(new ArrayList<>())
                .totalTagihan(BigDecimal.ZERO)
                .build();

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (OrderRequest.ItemRequest itemReq : request.getItems()) {
            
            ProductBatch batch = productBatchRepository.findById(itemReq.getBatchId())
                    .orElseThrow(() -> new RuntimeException("Data Corrupt: Batch ID " + itemReq.getBatchId() + " tidak ditemukan!"));

            OrderItem orderItem = tieringPriceService.processB2BCheckoutItem(
                    customer,
                    order,
                    batch.getProduct().getIdProduk(),
                    itemReq.getQuantity()
            );

            grandTotal = grandTotal.add(orderItem.getSubtotalItem());
            order.getItems().add(orderItem);
        }

        order.setTotalTagihan(grandTotal.add(order.getOngkir()));
        return orderRepository.save(order);
    }
}