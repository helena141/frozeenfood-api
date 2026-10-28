package com.lezzat.b2b2c.frozenfood.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lezzat.b2b2c.frozenfood.dto.request.OrderRequest;
import com.lezzat.b2b2c.frozenfood.entity.Customer;
import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.entity.OrderItem;
import com.lezzat.b2b2c.frozenfood.repository.CustomerRepository;
import com.lezzat.b2b2c.frozenfood.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final TieringPriceService tieringPriceService;

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
            TieringPriceService.B2BCheckoutResult checkoutResult = tieringPriceService.executeFefoBlendedPricing(
                    customer,
                    itemReq.getIdProduk(),
                    itemReq.getQuantity()
            );

            for (TieringPriceService.BatchDeduction deduction : checkoutResult.deductedBatches()) {
                BigDecimal subtotalItem = checkoutResult.blendedUnitPrice()
                        .multiply(BigDecimal.valueOf(deduction.quantityDeducted()))
                        .setScale(2, RoundingMode.HALF_UP);

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .productBatch(deduction.batch())
                        .quantity(deduction.quantityDeducted())
                        .hargaSatuanSnapshot(checkoutResult.blendedUnitPrice())
                        .subtotalItem(subtotalItem)
                        .build();

                order.getItems().add(orderItem);
                grandTotal = grandTotal.add(subtotalItem);
            }
        }

        order.setTotalTagihan(grandTotal.add(order.getOngkir()));
        return orderRepository.save(order);
    }
}
