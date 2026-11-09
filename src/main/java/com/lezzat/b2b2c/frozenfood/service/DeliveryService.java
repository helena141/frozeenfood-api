package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.request.DeliveryEstimateRequest;
import com.lezzat.b2b2c.frozenfood.dto.request.DeliveryKonfirmasiRequest;
import com.lezzat.b2b2c.frozenfood.dto.request.DeliveryRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.DeliveryEstimateResponse;
import com.lezzat.b2b2c.frozenfood.dto.response.DeliveryResponse;
import com.lezzat.b2b2c.frozenfood.entity.Delivery;
import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.entity.Product;
import com.lezzat.b2b2c.frozenfood.exception.DuplicateDeliveryException;
import com.lezzat.b2b2c.frozenfood.exception.InvalidDeliveryStatusException;
import com.lezzat.b2b2c.frozenfood.repository.DeliveryRepository;
import com.lezzat.b2b2c.frozenfood.repository.OrderRepository;
import com.lezzat.b2b2c.frozenfood.repository.ProductRepository;
import com.lezzat.b2b2c.frozenfood.service.core.DeliveryPlanner;
import com.lezzat.b2b2c.frozenfood.service.core.DeliveryProperties;
import com.lezzat.b2b2c.frozenfood.service.core.DeliveryStatusMachine;
import com.lezzat.b2b2c.frozenfood.service.core.MuatanCalculator.ItemMuatan;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DeliveryService hanya BACA Order (milik Moreno), tidak mengubahnya.
 * Ongkir disimpan di Delivery saja; integrasi ke total Order/nota = koordinasi tim.
 * Waktu "pesan" untuk batas jam 15.00 memakai waktu pengiriman dibuat
 * (proxy; ganti ke Order.createdAt bila tipenya sudah dipastikan).
 */
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DeliveryPlanner planner;
    private final DeliveryProperties props;

    private ZonedDateTime sekarang() {
        return ZonedDateTime.now(ZoneId.of(props.getSchedule().getZone()));
    }

    /** Pratinjau: tidak menyimpan apa pun. */
    @Transactional(readOnly = true)
    public DeliveryEstimateResponse estimate(DeliveryEstimateRequest req) {
        List<ItemMuatan> items;
        if (req.idOrder() != null) {
            items = itemsDariOrder(findOrder(req.idOrder()));
        } else {
            items = new ArrayList<>();
            if (req.items() != null) {
                for (DeliveryEstimateRequest.Item it : req.items()) {
                    Product p = productRepository.findById(it.idProduk())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Produk dengan id " + it.idProduk() + " tidak ditemukan"));
                    items.add(new ItemMuatan(p.getNamaProduk(), p.getBerat(), p.getVolume(), it.quantity()));
                }
            }
        }
        return planner.rencanakan(req.latitudeTujuan(), req.longitudeTujuan(), items, sekarang());
    }

    @Transactional
    public DeliveryResponse createDelivery(DeliveryRequest request) {
        Order order = findOrder(request.idOrder());
        if (deliveryRepository.findByOrder_IdOrder(request.idOrder()).isPresent()) {
            throw new DuplicateDeliveryException("Order " + request.idOrder() + " sudah memiliki pengiriman");
        }

        DeliveryEstimateResponse plan = planner.rencanakan(
                request.latitudeTujuan(), request.longitudeTujuan(), itemsDariOrder(order), sekarang());

        boolean pihakKetiga = plan.perluKonfirmasiAdmin();
        String metode = pihakKetiga ? "PIHAK_KETIGA" : (plan.jadwalHariIni() ? "SAMEDAY" : "TERJADWAL");
        LocalDateTime now = LocalDateTime.now();

        Delivery delivery = Delivery.builder()
                .order(order)
                .alamatTujuan(request.alamatTujuan())
                .jarak(plan.jarakKm().floatValue())
                .metodeDeliv(metode)
                .statusDelivery(pihakKetiga ? DeliveryStatusMachine.MENUNGGU_KONFIRMASI_ADMIN
                        : DeliveryStatusMachine.MENUNGGU_PICKUP)
                .pelaksana(plan.pelaksana())
                .ongkir(pihakKetiga ? java.math.BigDecimal.ZERO.setScale(2) : plan.ongkir())
                .sumberJarak(plan.sumberJarak())
                .estimasiTiba(plan.estimasiTiba())
                .alasanKeputusan(String.join("; ", plan.alasan()))
                .updatedAt(now)
                .build();
        return toResponse(deliveryRepository.save(delivery));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getById(UUID idDelivery) {
        return toResponse(findOrThrow(idDelivery));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getByOrderId(UUID idOrder) {
        return toResponse(deliveryRepository.findByOrder_IdOrder(idOrder)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Delivery untuk order " + idOrder + " tidak ditemukan")));
    }

    @Transactional
    public DeliveryResponse updateStatus(UUID idDelivery, String statusBaru) {
        Delivery d = findOrThrow(idDelivery);
        String target = DeliveryStatusMachine.normalisasi(statusBaru);
        DeliveryStatusMachine.validasiTransisi(d.getStatusDelivery(), target);
        if (DeliveryStatusMachine.MENUNGGU_KONFIRMASI_ADMIN.equals(d.getStatusDelivery())
                && DeliveryStatusMachine.MENUNGGU_PICKUP.equals(target)) {
            throw new InvalidDeliveryStatusException(
                    "Pengiriman pihak ketiga harus dikonfirmasi lewat PATCH /api/deliveries/{id}/konfirmasi-admin "
                            + "(isi ongkir dan nama pengantar)");
        }
        d.setStatusDelivery(target);
        d.setUpdatedAt(LocalDateTime.now());
        return toResponse(d);
    }

    /** Owner/admin: isi ongkir pihak ketiga manual, lalu lanjut ke MENUNGGU_PICKUP. */
    @Transactional
    public DeliveryResponse konfirmasiAdmin(UUID idDelivery, DeliveryKonfirmasiRequest req) {
        Delivery d = findOrThrow(idDelivery);
        if (!DeliveryStatusMachine.MENUNGGU_KONFIRMASI_ADMIN.equals(d.getStatusDelivery())) {
            throw new InvalidDeliveryStatusException(
                    "Konfirmasi admin hanya untuk pengiriman berstatus MENUNGGU_KONFIRMASI_ADMIN (saat ini: "
                            + d.getStatusDelivery() + ")");
        }
        d.setOngkir(req.ongkir().setScale(2, java.math.RoundingMode.HALF_UP));
        d.setNamaPengantar(req.namaPengantar().trim());
        d.setStatusDelivery(DeliveryStatusMachine.MENUNGGU_PICKUP);
        d.setUpdatedAt(LocalDateTime.now());
        return toResponse(d);
    }

    @Transactional
    public DeliveryResponse setPengantar(UUID idDelivery, String nama) {
        Delivery d = findOrThrow(idDelivery);
        if (DeliveryStatusMachine.statusAkhir(d.getStatusDelivery())) {
            throw new InvalidDeliveryStatusException(
                    "Pengiriman berstatus " + d.getStatusDelivery() + " tidak dapat diubah lagi");
        }
        d.setNamaPengantar(nama.trim());
        d.setUpdatedAt(LocalDateTime.now());
        return toResponse(d);
    }

    private List<ItemMuatan> itemsDariOrder(Order order) {
        List<ItemMuatan> items = new ArrayList<>();
        order.getItems().forEach(it -> {
            Product p = it.getProductBatch().getProduct();
            items.add(new ItemMuatan(p.getNamaProduk(), p.getBerat(), p.getVolume(), it.getQuantity()));
        });
        return items;
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Order dengan id " + id + " tidak ditemukan"));
    }

    private Delivery findOrThrow(UUID id) {
        return deliveryRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Delivery dengan id " + id + " tidak ditemukan"));
    }

    private DeliveryResponse toResponse(Delivery d) {
        return new DeliveryResponse(d.getIdDelivery(), d.getOrder().getIdOrder(), d.getAlamatTujuan(),
                d.getJarak(), d.getMetodeDeliv(), d.getStatusDelivery(), d.getCreatedAt(), d.getUpdatedAt(),
                d.getPelaksana(), d.getOngkir(), d.getSumberJarak(), d.getEstimasiTiba(),
                d.getNamaPengantar(), d.getAlasanKeputusan());
    }
}
