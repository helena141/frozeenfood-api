package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.entity.Delivery;
import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.entity.OrderItem;
import com.lezzat.b2b2c.frozenfood.entity.Payment;
import com.lezzat.b2b2c.frozenfood.repository.DeliveryRepository;
import com.lezzat.b2b2c.frozenfood.repository.OrderRepository;
import com.lezzat.b2b2c.frozenfood.repository.PaymentRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * UC-17 / UC-26: Unduh Nota Pesanan (Satuan & Grosir), versi Smart Delivery.
 *
 * Perubahan utama dari versi lama:
 *  - Ongkir dibaca dari tabel deliveries (hasil Smart Delivery), BUKAN dari
 *    Order.ongkir yang selalu 0 (placeholder). Order tidak diubah.
 *  - Total nota = subtotal barang + ongkir dari Delivery.
 *  - Ada blok pengiriman (alamat, jarak, pelaksana, pengantar, status, estimasi tiba).
 *  - Jumlah dibayar diambil dari Payment APA ADANYA. Bila berbeda dari total nota,
 *    hanya ditampilkan catatan informasi (tidak ada perhitungan selisih tagihan).
 *
 * ASUMSI (sama seperti sebelumnya): Customer.getNama()/getEmail(); Payment yang
 * ditampilkan = yang TERBARU.
 */
@Service
@RequiredArgsConstructor
public class NotaPdfService {

    private static final DateTimeFormatter TANGGAL = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final Color ABU = new Color(230, 230, 230);

    private static final Font F_JUDUL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font F_SUB = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font F_BAB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font F_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font F_ISI = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font F_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font F_CATATAN = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final TieringPriceService tieringPriceService;

    /**
     * Password pemilik PDF (untuk mengunci izin edit). Bisa diatur di application.yml:
     * lezzat.nota.owner-password. Kalau tidak diisi, dipakai acak per aplikasi dijalankan
     * (tidak ada yang perlu mengetahuinya, nota tetap bisa dibuka & dicetak tanpa password).
     */
    @Value("${lezzat.nota.owner-password:#{T(java.util.UUID).randomUUID().toString()}}")
    private String ownerPassword;

    @Transactional(readOnly = true)
    public byte[] generateNotaPdf(UUID idOrder) {
        Order order = orderRepository.findByIdWithItems(idOrder)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order dengan id " + idOrder + " tidak ditemukan"));

        Payment payment = paymentRepository.findByOrder_IdOrder(idOrder).stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .orElse(null);
        Delivery delivery = deliveryRepository.findByOrder_IdOrder(idOrder).orElse(null);

        try {
            return buildPdf(order, payment, delivery);
        } catch (Exception e) {
            throw new RuntimeException("Gagal membuat PDF nota: " + e.getMessage(), e);
        }
    }

    private byte[] buildPdf(Order order, Payment payment, Delivery delivery) throws Exception {
        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        // Kunci PDF: boleh dibuka, dilihat, dicetak, dibaca screen reader;
        // TIDAK boleh diubah, disalin isinya, atau diberi anotasi. Harus dipanggil sebelum doc.open().
        writer.setEncryption(null, ownerPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                PdfWriter.ALLOW_PRINTING | PdfWriter.ALLOW_SCREENREADERS,
                PdfWriter.STANDARD_ENCRYPTION_128);
        doc.open();

        // ===== Header =====
        Paragraph judul = new Paragraph("LEZZAT FROZEN FOOD", F_JUDUL);
        judul.setAlignment(Element.ALIGN_CENTER);
        doc.add(judul);
        Paragraph sub = new Paragraph("NOTA " + labelTipe(order.getTipeOrder()).toUpperCase(Locale.ROOT), F_SUB);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(14);
        doc.add(sub);

        // ===== Info pesanan =====
        PdfPTable info = new PdfPTable(new float[]{1.4f, 4f});
        info.setWidthPercentage(100);
        baris(info, "No. Pesanan", order.getIdOrder().toString());
        baris(info, "Tanggal", order.getCreatedAt() != null ? order.getCreatedAt().format(TANGGAL) : "-");
        baris(info, "Status Pesanan", order.getStatus());
        baris(info, "Pelanggan", infoPelanggan(order));
        if (order.getCatatan() != null && !order.getCatatan().isBlank()) {
            baris(info, "Catatan", order.getCatatan());
        }
        doc.add(info);

        // ===== Tabel barang =====
        PdfPTable tabel = new PdfPTable(new float[]{3.6f, 0.8f, 1.8f, 2.0f, 2.0f});
        tabel.setWidthPercentage(100);
        tabel.setSpacingBefore(14);
        tabel.setHeaderRows(1);
        header(tabel, "Produk & Batch FEFO", Element.ALIGN_LEFT);
        header(tabel, "Qty", Element.ALIGN_CENTER);
        header(tabel, "Harga Normal", Element.ALIGN_RIGHT);
        header(tabel, "Harga Deal/Tier", Element.ALIGN_RIGHT);
        header(tabel, "Subtotal", Element.ALIGN_RIGHT);

        BigDecimal subtotalBarang = BigDecimal.ZERO;
        BigDecimal totalHargaNormal = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            String nama = item.getProductBatch() != null && item.getProductBatch().getProduct() != null
                    ? item.getProductBatch().getProduct().getNamaProduk() : "(produk tidak diketahui)";
            String kodeBatch = item.getProductBatch() != null ? item.getProductBatch().getIdProdBatch() : "-";
            BigDecimal hargaNormal = (item.getProductBatch() != null && item.getProductBatch().getProduct() != null
                    && item.getProductBatch().getProduct().getHargaEceran() != null)
                    ? item.getProductBatch().getProduct().getHargaEceran()
                    : item.getHargaSatuanSnapshot();
            BigDecimal hargaFinal = item.getHargaSatuanSnapshot() != null
                    ? item.getHargaSatuanSnapshot()
                    : BigDecimal.ZERO;
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;

            if (hargaNormal != null) {
                totalHargaNormal = totalHargaNormal.add(hargaNormal.multiply(BigDecimal.valueOf(qty)));
            }

            String infoDiskon = "";
            if (hargaNormal != null && hargaNormal.compareTo(BigDecimal.ZERO) > 0
                    && hargaFinal.compareTo(hargaNormal) < 0) {
                BigDecimal pct = hargaNormal.subtract(hargaFinal)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(hargaNormal, 0, java.math.RoundingMode.HALF_UP);
                TieringPriceService.BatchPriceEvaluation batchEval = tieringPriceService.evaluateBatchPrice(
                        item.getProductBatch().getProduct(), item.getProductBatch());
                if (batchEval.isDealBatch() && hargaFinal.compareTo(batchEval.hargaEfektif()) == 0
                        && batchEval.labelDeal() != null) {
                    infoDiskon = "\n[" + batchEval.labelDeal() + "]";
                } else {
                    infoDiskon = "\n[Grosir/Diskon -" + pct.stripTrailingZeros().toPlainString() + "%]";
                }
            }

            isi(tabel, nama + "\nBatch: " + kodeBatch + infoDiskon, Element.ALIGN_LEFT);
            isi(tabel, String.valueOf(qty), Element.ALIGN_CENTER);
            isi(tabel, rupiah(hargaNormal), Element.ALIGN_RIGHT);
            isi(tabel, rupiah(hargaFinal), Element.ALIGN_RIGHT);
            isi(tabel, rupiah(item.getSubtotalItem()), Element.ALIGN_RIGHT);
            if (item.getSubtotalItem() != null) {
                subtotalBarang = subtotalBarang.add(item.getSubtotalItem());
            }
        }
        doc.add(tabel);

        // ===== Ringkasan biaya =====
        boolean ongkirBelumPasti = delivery != null
                && "MENUNGGU_KONFIRMASI_ADMIN".equals(delivery.getStatusDelivery());
        BigDecimal ongkir = (delivery != null && delivery.getOngkir() != null && !ongkirBelumPasti)
                ? delivery.getOngkir() : BigDecimal.ZERO;
        BigDecimal total = subtotalBarang.add(ongkir);
        BigDecimal totalHemat = totalHargaNormal.subtract(subtotalBarang);

        PdfPTable ringkas = new PdfPTable(new float[]{3.2f, 2f});
        ringkas.setWidthPercentage(60);
        ringkas.setHorizontalAlignment(Element.ALIGN_RIGHT);
        ringkas.setSpacingBefore(8);
        if (totalHemat.compareTo(BigDecimal.ZERO) > 0) {
            barisBiaya(ringkas, "Total Harga Normal", rupiah(totalHargaNormal), F_ISI);
            barisBiaya(ringkas, "Total Hemat (Diskon / Tiering)", "- " + rupiah(totalHemat), F_LABEL);
        }
        barisBiaya(ringkas, "Subtotal Barang", rupiah(subtotalBarang), F_ISI);
        String labelOngkir = ongkirBelumPasti ? "Menunggu konfirmasi"
                : (delivery == null ? "Belum ada data pengiriman"
                : (ongkir.signum() == 0 ? "GRATIS" : rupiah(ongkir)));
        barisBiaya(ringkas, "Ongkos Kirim", labelOngkir, F_ISI);
        barisBiaya(ringkas, "TOTAL TAGIHAN", rupiah(total), F_TOTAL);
        doc.add(ringkas);

        if (ongkirBelumPasti) {
            Paragraph p = new Paragraph("Ongkir pengiriman pihak ketiga masih menunggu konfirmasi admin; "
                    + "total di atas belum termasuk ongkir tersebut.", F_CATATAN);
            p.setAlignment(Element.ALIGN_RIGHT);
            p.setSpacingBefore(4);
            doc.add(p);
        }

        // ===== Pengiriman =====
        doc.add(bab("Pengiriman"));
        if (delivery == null) {
            doc.add(new Paragraph("Belum ada data pengiriman untuk pesanan ini.", F_ISI));
        } else {
            PdfPTable kirim = new PdfPTable(new float[]{1.4f, 4f});
            kirim.setWidthPercentage(100);
            baris(kirim, "Alamat Tujuan", delivery.getAlamatTujuan());
            baris(kirim, "Jarak", delivery.getJarak() != null
                    ? String.format(Locale.US, "%.1f km", delivery.getJarak()).replace('.', ',') : "-");
            baris(kirim, "Pelaksana", labelPelaksana(delivery.getPelaksana()));
            baris(kirim, "Pengantar", namaPengantar(delivery));
            baris(kirim, "Status", delivery.getStatusDelivery());
            baris(kirim, "Estimasi Tiba", delivery.getEstimasiTiba() != null
                    ? delivery.getEstimasiTiba().format(TANGGAL) + " WIB" : "-");
            doc.add(kirim);
        }

        // ===== Pembayaran =====
        doc.add(bab("Pembayaran"));
        if (payment == null) {
            doc.add(new Paragraph("Belum ada data pembayaran.", F_ISI));
        } else {
            PdfPTable bayar = new PdfPTable(new float[]{1.4f, 4f});
            bayar.setWidthPercentage(100);
            baris(bayar, "Metode", payment.getMetodePay());
            baris(bayar, "Status", payment.getStatus());
            baris(bayar, "Jumlah Dibayar", rupiah(payment.getJumlah()));
            doc.add(bayar);
            if (payment.getJumlah() != null && payment.getJumlah().compareTo(total) != 0) {
                Paragraph p = new Paragraph("Catatan: jumlah pembayaran tercatat berbeda dari total tagihan nota. "
                        + "Mohon dikonfirmasi ke admin bila diperlukan.", F_CATATAN);
                p.setSpacingBefore(4);
                doc.add(p);
            }
        }

        Paragraph footer = new Paragraph("Terima kasih telah berbelanja di Lezzat Frozen Food.", F_CATATAN);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(28);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    // ----- helper tampilan -----

    private Paragraph bab(String teks) {
        Paragraph p = new Paragraph(teks, F_BAB);
        p.setSpacingBefore(16);
        p.setSpacingAfter(4);
        return p;
    }

    private void baris(PdfPTable t, String label, String nilai) {
        PdfPCell a = new PdfPCell(new Phrase(label, F_LABEL));
        a.setBorder(Rectangle.NO_BORDER);
        a.setPadding(2);
        PdfPCell b = new PdfPCell(new Phrase(nilai != null ? nilai : "-", F_ISI));
        b.setBorder(Rectangle.NO_BORDER);
        b.setPadding(2);
        t.addCell(a);
        t.addCell(b);
    }

    private void header(PdfPTable t, String teks, int align) {
        PdfPCell c = new PdfPCell(new Phrase(teks, F_LABEL));
        c.setBackgroundColor(ABU);
        c.setPadding(6);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void isi(PdfPTable t, String teks, int align) {
        PdfPCell c = new PdfPCell(new Phrase(teks, F_ISI));
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void barisBiaya(PdfPTable t, String label, String nilai, Font font) {
        PdfPCell a = new PdfPCell(new Phrase(label, font));
        a.setBorder(Rectangle.NO_BORDER);
        a.setPadding(3);
        PdfPCell b = new PdfPCell(new Phrase(nilai, font));
        b.setBorder(Rectangle.NO_BORDER);
        b.setPadding(3);
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(a);
        t.addCell(b);
    }

    private String infoPelanggan(Order order) {
        if (order.getCustomer() == null) {
            return "-";
        }
        try {
            String nama = order.getCustomer().getNama();
            String email = order.getCustomer().getEmail();
            return nama + (email != null ? " (" + email + ")" : "");
        } catch (Exception e) {
            return order.getCustomer().getIdUser().toString();
        }
    }

    private String labelTipe(String tipe) {
        return "B2B".equalsIgnoreCase(tipe) ? "Pesanan Grosir (B2B)" : "Pesanan Satuan (B2C)";
    }

    /** Nama pengantar; bila kosong: motor toko -> "Karyawan toko", pihak ketiga -> "Belum ditentukan". */
    private String namaPengantar(Delivery d) {
        if (d.getNamaPengantar() != null && !d.getNamaPengantar().isBlank()) {
            return d.getNamaPengantar();
        }
        return "PIHAK_KETIGA".equals(d.getPelaksana()) ? "Belum ditentukan" : "Karyawan toko";
    }

    private String labelPelaksana(String p) {
        if ("PIHAK_KETIGA".equals(p)) {
            return "Pihak ketiga";
        }
        return "Karyawan toko (motor)";
    }

    /** Format Indonesia: Rp 1.234.500 (tanpa desimal bila bulat). */
    private String rupiah(BigDecimal v) {
        if (v == null) {
            return "Rp 0";
        }
        boolean bulat = v.stripTrailingZeros().scale() <= 0;
        String s = String.format(Locale.US, bulat ? "%,.0f" : "%,.2f", v);
        s = s.replace(',', '#').replace('.', ',').replace('#', '.');
        return "Rp " + s;
    }
}