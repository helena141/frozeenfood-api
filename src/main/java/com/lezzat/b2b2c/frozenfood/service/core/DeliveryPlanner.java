package com.lezzat.b2b2c.frozenfood.service.core;

import com.lezzat.b2b2c.frozenfood.dto.response.DeliveryEstimateResponse;
import com.lezzat.b2b2c.frozenfood.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Otak Smart Delivery: memutuskan pelaksana (motor toko / pihak ketiga), ongkir,
 * dan jadwal. Hanya menghitung, tidak menyimpan apa pun.
 *
 * Urutan aturan pelaksana (yang pertama kena menang):
 *  1. jarak > radius motor (15 km)          -> PIHAK_KETIGA
 *  2. muatan > kapasitas satu motor         -> PIHAK_KETIGA
 *  3. waktu tempuh > batas aman barang beku -> PIHAK_KETIGA
 *  4. selain itu                            -> MOTOR_TOKO
 * Pihak ketiga selalu perlu konfirmasi admin; ongkir dari rumus hanya estimasi.
 */
@Service
@RequiredArgsConstructor
public class DeliveryPlanner {

    private final SmartDeliveryService smartDeliveryService;
    private final DeliveryProperties props;
    private final DeliveryRepository deliveryRepository;

    public DeliveryEstimateResponse rencanakan(double lat, double lon,
                                               List<MuatanCalculator.ItemMuatan> items,
                                               ZonedDateTime sekarang) {
        DeliveryProperties.Fee f = props.getFee();
        DeliveryProperties.Motor m = props.getMotor();
        DeliveryProperties.Schedule sc = props.getSchedule();

        SmartDeliveryService.JarakHasil jarak = smartDeliveryService.hitungJarak(lat, lon);
        DeliveryFeeCalculator fee = new DeliveryFeeCalculator(f.getFreeKm(), f.getBaseFee(), f.getStepFee(), f.getMaxFee());
        MuatanCalculator.Muatan muatan = new MuatanCalculator(
                props.getUnit().getWeightToKg(), props.getUnit().getVolumeToLiter()).hitung(items);

        long sibukCount = deliveryRepository.countByPelaksanaAndStatusDeliveryIn(
                "MOTOR_TOKO", List.of(DeliveryStatusMachine.DISIAPKAN, DeliveryStatusMachine.DALAM_PERJALANAN));
        boolean semuaSibuk = sibukCount >= m.getDrivers();

        DeliveryScheduler scheduler = new DeliveryScheduler(sc.getOpen(), sc.getClose(), sc.getSameDayCutoff(),
                ZoneId.of(sc.getZone()), m.getPrepMinutes(), m.getAvgSpeedKmh(), m.getQueueDelayMinutes());
        int menitTempuh = scheduler.menitPerjalanan(jarak.jarakKm());

        List<String> alasan = new ArrayList<>();
        List<String> peringatan = new ArrayList<>(muatan.peringatan());
        if ("PERKIRAAN".equals(jarak.sumber())) {
            peringatan.add("Jarak berupa perkiraan (garis lurus x faktor jalan), bukan rute asli");
        }

        String pelaksana = "MOTOR_TOKO";
        boolean muat = MuatanCalculator.muatDiSatuMotor(muatan, m.getMaxWeightKg(), m.getMaxVolumeLiter());
        if (jarak.jarakKm().doubleValue() > smartDeliveryService.getMaxRadiusKm()) {
            pelaksana = "PIHAK_KETIGA";
            alasan.add("Jarak " + jarak.jarakKm() + " km melebihi jangkauan motor toko "
                    + smartDeliveryService.getMaxRadiusKm() + " km");
        } else if (!muat) {
            pelaksana = "PIHAK_KETIGA";
            alasan.add("Muatan " + muatan.beratKg() + " kg / " + muatan.volumeLiter()
                    + " liter melebihi kapasitas satu motor (" + m.getMaxWeightKg() + " kg / "
                    + m.getMaxVolumeLiter() + " liter)");
        } else if (menitTempuh > m.getMaxColdChainMinutes()) {
            pelaksana = "PIHAK_KETIGA";
            alasan.add("Waktu tempuh " + menitTempuh + " menit melebihi batas aman barang beku "
                    + m.getMaxColdChainMinutes() + " menit");
        } else {
            alasan.add("Dalam jangkauan, muatan muat di satu motor, waktu tempuh aman");
        }

        boolean pihakKetiga = "PIHAK_KETIGA".equals(pelaksana);
        DeliveryScheduler.Jadwal jadwal = scheduler.jadwalkan(sekarang, jarak.jarakKm(), !pihakKetiga && semuaSibuk);
        alasan.add(jadwal.keterangan());
        if (pihakKetiga) {
            peringatan.add("Perlu konfirmasi admin: ongkir pihak ketiga diisi manual oleh owner; "
                    + "angka ongkir di sini hanya estimasi dari rumus motor");
        }

        return new DeliveryEstimateResponse(
                jarak.jarakKm(), jarak.sumber(), fee.bulatkanKm(jarak.jarakKm()),
                pelaksana, pihakKetiga,
                fee.hitung(jarak.jarakKm()), pihakKetiga,
                jadwal.hariIni(), jadwal.estimasiTiba(), jadwal.menitPerjalanan(),
                muatan.beratKg(), muatan.volumeLiter(), alasan, peringatan);
    }
}
