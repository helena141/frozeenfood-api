package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.entity.OrderItem;
import com.lezzat.b2b2c.frozenfood.entity.Payment;
import com.lezzat.b2b2c.frozenfood.repository.OrderRepository;
import com.lezzat.b2b2c.frozenfood.repository.PaymentRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * UC-17 / UC-26: Unduh Nota Pesanan (Satuan & Grosir).
 * Satu service generik untuk kedua UC karena Order sudah generik
 * (dibedakan lewat tipeOrder: B2C/B2B) — tidak perlu 2 service terpisah.
 *
 * ASUMSI yang dipakai di sini, tolong dikonfirmasi/sesuaikan:
 * - customer.getNama() dan customer.getEmail() — field Customer.java belum
 *   pernah saya lihat lengkap, cuma tahu field idUser dari kode sebelumnya.
 *   Kalau nama getter aslinya beda, cukup diganti di method buildCustomerInfo().
 * - Payment yang ditampilkan di nota: Payment TERBARU (createdAt paling akhir)
 *   untuk Order ini, kalau ada lebih dari satu percobaan pembayaran.
 */
@Service
@RequiredArgsConstructor
public class NotaPdfService {

    private static final DateTimeFormatter TANGGAL_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public byte[] generateNotaPdf(UUID idOrder) {
        Order order = orderRepository.findByIdWithItems(idOrder)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order dengan id " + idOrder + " tidak ditemukan"));

        Payment paymentTerbaru = paymentRepository.findByOrder_IdOrder(idOrder).stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .orElse(null);

        try {
            return buildPdf(order, paymentTerbaru);
        } catch (Exception e) {
            // Dibungkus RuntimeException supaya ditangkap GlobalExceptionHandler
            // sebagai 500 dengan pesan jelas, bukan stack trace OpenPDF mentah.
            throw new RuntimeException("Gagal membuat PDF nota: " + e.getMessage(), e);
        }
    }

    private byte[] buildPdf(Order order, Payment payment) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font fontJudul = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font fontSubjudul = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 11);
        Font fontHeaderTabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font fontIsiTabel = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph judul = new Paragraph("NOTA PESANAN - LEZZAT FROZEN FOOD", fontJudul);
        judul.setAlignment(Element.ALIGN_CENTER);
        document.add(judul);

        Paragraph subjudul = new Paragraph(
                "Jenis Pesanan: " + labelTipeOrder(order.getTipeOrder()), fontSubjudul);
        subjudul.setAlignment(Element.ALIGN_CENTER);
        subjudul.setSpacingAfter(16);
        document.add(subjudul);

        document.add(buildInfoParagraph("No. Pesanan", order.getIdOrder().toString(), fontLabel, fontNormal));
        document.add(buildInfoParagraph("Tanggal", order.getCreatedAt() != null
                ? order.getCreatedAt().format(TANGGAL_FORMAT) : "-", fontLabel, fontNormal));
        document.add(buildInfoParagraph("Status Pesanan", order.getStatus(), fontLabel, fontNormal));
        document.add(buildInfoParagraph("Pelanggan", buildCustomerInfo(order), fontLabel, fontNormal));

        if (order.getCatatan() != null && !order.getCatatan().isBlank()) {
            document.add(buildInfoParagraph("Catatan", order.getCatatan(), fontLabel, fontNormal));
        }

        Paragraph spacer = new Paragraph(" ");
        document.add(spacer);

        PdfPTable table = new PdfPTable(new float[]{4f, 1.3f, 2f, 2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);

        addHeaderCell(table, "Produk", fontHeaderTabel);
        addHeaderCell(table, "Qty", fontHeaderTabel);
        addHeaderCell(table, "Harga Satuan", fontHeaderTabel);
        addHeaderCell(table, "Subtotal", fontHeaderTabel);

        List<OrderItem> items = order.getItems();
        for (OrderItem item : items) {
            String namaProduk = item.getProductBatch() != null && item.getProductBatch().getProduct() != null
                    ? item.getProductBatch().getProduct().getNamaProduk()
                    : "(produk tidak diketahui)";

            table.addCell(new PdfPCell(new Phrase(namaProduk, fontIsiTabel)));

            PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), fontIsiTabel));
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(qtyCell);

            PdfPCell hargaCell = new PdfPCell(new Phrase(formatRupiah(item.getHargaSatuanSnapshot()), fontIsiTabel));
            hargaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(hargaCell);

            PdfPCell subtotalCell = new PdfPCell(new Phrase(formatRupiah(item.getSubtotalItem()), fontIsiTabel));
            subtotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(subtotalCell);
        }

        document.add(table);

        Paragraph ongkir = new Paragraph(
                "Ongkos Kirim: " + formatRupiah(order.getOngkir()), fontNormal);
        ongkir.setAlignment(Element.ALIGN_RIGHT);
        ongkir.setSpacingBefore(10);
        document.add(ongkir);

        Paragraph total = new Paragraph(
                "TOTAL TAGIHAN: " + formatRupiah(order.getTotalTagihan()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingAfter(16);
        document.add(total);

        if (payment != null) {
            document.add(buildInfoParagraph("Metode Pembayaran", payment.getMetodePay(), fontLabel, fontNormal));
            document.add(buildInfoParagraph("Status Pembayaran", payment.getStatus(), fontLabel, fontNormal));
        } else {
            document.add(buildInfoParagraph("Status Pembayaran", "Belum ada data pembayaran", fontLabel, fontNormal));
        }

        Paragraph footer = new Paragraph(
                "\nTerima kasih telah berbelanja di Lezzat Frozen Food.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(24);
        document.add(footer);

        document.close();
        return outputStream.toByteArray();
    }

    private String buildCustomerInfo(Order order) {
        if (order.getCustomer() == null) {
            return "-";
        }
        // ASUMSI nama getter: getNama()/getEmail(). Sesuaikan kalau beda
        // dengan field asli di Customer.java.
        try {
            String nama = order.getCustomer().getNama();
            String email = order.getCustomer().getEmail();
            return nama + (email != null ? " (" + email + ")" : "");
        } catch (Exception e) {
            return order.getCustomer().getIdUser().toString();
        }
    }

    private String labelTipeOrder(String tipeOrder) {
        if ("B2B".equalsIgnoreCase(tipeOrder)) {
            return "Pesanan Grosir (B2B)";
        }
        return "Pesanan Satuan (B2C)";
    }

    private Paragraph buildInfoParagraph(String label, String value, Font fontLabel, Font fontNormal) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(label + ": ", fontLabel));
        p.add(new Phrase(value != null ? value : "-", fontNormal));
        p.setSpacingAfter(4);
        return p;
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private String formatRupiah(BigDecimal value) {
        if (value == null) {
            return "Rp 0";
        }
        return "Rp " + String.format("%,.2f", value).replace(',', '.');
    }
}
