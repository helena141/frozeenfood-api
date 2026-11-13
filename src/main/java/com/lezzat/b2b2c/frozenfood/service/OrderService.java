package com.lezzat.b2b2c.frozenfood.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lezzat.b2b2c.frozenfood.dto.request.OrderRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.OrderItemResponse;
import com.lezzat.b2b2c.frozenfood.dto.response.OrderResponse;
import com.lezzat.b2b2c.frozenfood.entity.Customer;
import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.entity.OrderItem;
import com.lezzat.b2b2c.frozenfood.repository.CustomerRepository;
import com.lezzat.b2b2c.frozenfood.repository.OrderRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final TieringPriceService tieringPriceService;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kriminal: Token valid tapi user tidak ada di Database!"));

        Order order = Order.builder()
                .customer(customer)
                .tipeOrder(role)
                .status("MENUNGGU_PEMBAYARAN")
                .ongkir(BigDecimal.ZERO)
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
                BigDecimal hargaSatuanBatch = deduction.rawUnitPrice();
                BigDecimal subtotalItem = deduction.rawSubtotal();

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .productBatch(deduction.batch())
                        .quantity(deduction.quantityDeducted())
                        .hargaSatuanSnapshot(hargaSatuanBatch)
                        .subtotalItem(subtotalItem)
                        .build();

                order.getItems().add(orderItem);
                grandTotal = grandTotal.add(subtotalItem);
            }
        }

        order.setTotalTagihan(grandTotal.add(order.getOngkir()));
        Order savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Customer dengan email " + email + " tidak ditemukan"));

        return orderRepository.findByCustomerIdUserWithItems(customer.getIdUser())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID idOrder) {
        Order order = orderRepository.findByIdWithItems(idOrder)
                .orElseThrow(() -> new EntityNotFoundException("Order dengan ID " + idOrder + " tidak ditemukan"));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(String tipeOrder, String status) {
        return orderRepository.findAllWithItems().stream()
                .filter(o -> tipeOrder == null || tipeOrder.isBlank() || tipeOrder.equalsIgnoreCase(o.getTipeOrder()))
                .filter(o -> status == null || status.isBlank() || status.equalsIgnoreCase(o.getStatus()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID idOrder, String status) {
        Order order = orderRepository.findByIdWithItems(idOrder)
                .orElseThrow(() -> new EntityNotFoundException("Order dengan ID " + idOrder + " tidak ditemukan"));
        order.setStatus(status);
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    BigDecimal hargaAsli = item.getProductBatch().getProduct().getHargaEceran();
                    BigDecimal hargaSnapshot = item.getHargaSatuanSnapshot();
                    BigDecimal diskonPct = BigDecimal.ZERO;
                    if (hargaAsli != null && hargaAsli.compareTo(BigDecimal.ZERO) > 0
                            && hargaSnapshot != null && hargaSnapshot.compareTo(hargaAsli) < 0) {
                        diskonPct = hargaAsli.subtract(hargaSnapshot)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(hargaAsli, 0, RoundingMode.HALF_UP);
                    }
                    String ketPromo = resolveKeteranganPromo(item, hargaAsli, hargaSnapshot, diskonPct);
                    return new OrderItemResponse(
                            item.getIdOrderItem(),
                            item.getProductBatch().getProduct().getIdProduk(),
                            item.getProductBatch().getProduct().getNamaProduk(),
                            item.getProductBatch().getIdProdBatch(),
                            item.getQuantity(),
                            hargaAsli,
                            hargaSnapshot,
                            diskonPct,
                            ketPromo,
                            item.getSubtotalItem());
                })
                .toList();

        return new OrderResponse(
                order.getIdOrder(),
                order.getCustomer().getIdUser(),
                order.getTipeOrder(),
                order.getStatus(),
                order.getOngkir(),
                order.getTotalTagihan(),
                order.getCatatan(),
                itemResponses,
                order.getCreatedAt());
    }

    private String resolveKeteranganPromo(
            OrderItem item,
            BigDecimal hargaAsli,
            BigDecimal hargaSnapshot,
            BigDecimal diskonPct) {
        if (hargaAsli == null || hargaSnapshot == null || hargaSnapshot.compareTo(hargaAsli) >= 0) {
            return "Harga Normal";
        }
        TieringPriceService.BatchPriceEvaluation batchEval = tieringPriceService.evaluateBatchPrice(
                item.getProductBatch().getProduct(),
                item.getProductBatch()
        );
        if (batchEval.isDealBatch() && hargaSnapshot.compareTo(batchEval.hargaEfektif()) == 0
                && batchEval.labelDeal() != null) {
            return batchEval.labelDeal();
        }
        return "Harga Grosir / Diskon (-" + diskonPct.stripTrailingZeros().toPlainString() + "%)";
    }
}
