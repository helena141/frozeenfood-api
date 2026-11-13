# Dokumentasi Lengkap Perubahan Kode (Sebelum vs Sesudah): Sistem Expiry Alert Admin, FrozenFood FEFO Deals, Tiering B2B & Sinkronisasi Harga

**Proyek:** Lezzat Frozen Food (`frozenfood-api` & `lezzat-frontend`)  
**Tanggal Dokumentasi:** 13 November 2026  
**Cakupan:** Perbandingan kode **Sebelum Diubah (Before)** dan **Sesudah Diubah (After)** beserta penjelasan teknis lengkap untuk **Backend (Spring Boot)** dan **Frontend (Next.js)**.

---

## Daftar Isi

1. [Ringkasan Arsitektur yang Dibangun](#1-ringkasan-arsitektur-yang-dibangun)
2. [Bagian I — Perubahan Kode Backend (`frozenfood-api`)](#2-bagian-i--perubahan-kode-backend-frozenfood-api)
   - [1.1 `TieringPriceService.java` — Single Source of Truth Harga FEFO Deal, Clearance & Grosir](#11-tieringpriceservicejava--single-source-of-truth-harga-fefo-deal-clearance--grosir)
   - [1.2 `ExpiryAlertScheduler.java` — Otomasi Diskon H-30 (20%) & H-7 (50%) + Upgrade Alert](#12-expiryalertschedulerjava--otomasi-diskon-h-30-20--h-7-50--upgrade-alert)
   - [1.3 `ProductBatchService.java` — Trigger Instan Expiry Alert Saat Tambah/Edit Batch](#13-productbatchservicejava--trigger-instan-expiry-alert-saat-tambahedit-batch)
   - [1.4 `InventoryController.java` & `ExpiryAlertRepository.java` — Ekspos Data Harga Batch pada Alert Admin](#14-inventorycontrollerjava--expiryalertrepositoryjava--ekspos-data-harga-batch-pada-alert-admin)
   - [1.5 `ProductCatalogResponse.java` & `ProductService.java` — Ekspos Rincian Deal & `activeBatches` ke Katalog](#15-productcatalogresponsejava--productservicejava--ekspos-rincian-deal--activebatches-ke-katalog)
   - [1.6 `OrderItemResponse.java`, `OrderB2CService.java` & `OrderService.java` — Harga Deal & Tiering Saat Checkout + Rincian Promo](#16-orderitemresponsejava-orderb2cservicejava--orderservicejava--harga-deal--tiering-saat-checkout--rincian-promo)
   - [1.7 `NotaPdfService.java` — Tabel Harga Normal vs Harga Deal/Tier & Total Hemat di Nota PDF](#17-notapdfservicejava--tabel-harga-normal-vs-harga-dealtier--total-hemat-di-nota-pdf)
   - [1.8 `application.yml` — Perbaikan Key Konfigurasi Konversi Berat & Volume](#18-applicationyml--perbaikan-key-konfigurasi-konversi-berat--volume)
3. [Bagian II — Perubahan Kode Frontend (`lezzat-frontend`)](#3-bagian-ii--perubahan-kode-frontend-lezzat-frontend)
   - [2.1 `src/types/api.ts` — Kontrak TypeScript Baru untuk Deals, Batch Aktif, Tiering & Expiry Alert](#21-srctypesapits--kontrak-typescript-baru-untuk-deals-batch-aktif-tiering--expiry-alert)
   - [2.2 `src/app/admin/products/page.tsx` — Panel Expiry Alert Admin & Modal CRUD Tiering Grosir](#22-srcappadminproductspagetsx--panel-expiry-alert-admin--modal-crud-tiering-grosir)
   - [2.3 `src/app/page.tsx` — Banner "FrozenFood Deals" Murni dari Data Backend (Tanpa Angka Dummy)](#23-srcapppagetsx--banner-frozenfood-deals-murni-dari-data-backend-tanpa-angka-dummy)
   - [2.4 `src/components/shop/product-card.tsx` & `src/app/products/[id]/page.tsx` — Label Promo, Harga Coret & Tabel Tiering](#24-srccomponentsshopproduct-cardtsx--srcappproductsidpagetsx--label-promo-harga-coret--tabel-tiering)
   - [2.5 `src/lib/utils.ts` & `src/store/cart-store.ts` — Kalkulator Harga Multi-Batch FEFO (`evaluateCartItemPricing`)](#25-srclibutilsts--srcstorecart-storets--kalkulator-harga-multi-batch-fefo-evaluatecartitempricing)
   - [2.6 `src/components/shop/cart-panel.tsx`, `src/app/checkout/page.tsx` & `src/app/orders/[id]/page.tsx` — Sinkronisasi Rincian Diskon & Penghematan](#26-srccomponentsshopcart-paneltsx-srcappcheckoutpagetsx--srcappordersidpagetsx--sinkronisasi-rincian-diskon--penghematan)

---

## 1. Ringkasan Arsitektur yang Dibangun

Sebelum perubahan ini, terdapat beberapa celah antara Backend dan Frontend:
1. **Di Frontend Beranda (`src/app/page.tsx`):** Section *FEFO Flash Food Deals* masih mengandalkan angka *fallback* buatan frontend (diskon 15%–20% acak jika backend tidak mengirim field diskon), dan saat dimasukkan ke keranjang harga kembali ke `hargaEceran` atau menimpa `hargaEceran` secara lokal tetapi saat checkout ke backend tetap ditagih harga normal.
2. **Di Backend (`TieringPriceService`, `OrderB2CService`, `ExpiryAlertScheduler`):**
   - Batch yang mendekati expired **H-30 s/d H-8** hanya dibuatkan alert teks `DARURAT`, tetapi belum otomatis mendapat potongan harga **FEFO Deal 20%**.
   - Pembeli **B2C** pada `OrderB2CService` belum terhubung ke kalkulasi otomatis *Clearance H-7 (50%)*, *FEFO Deal H-30 (20%)*, maupun *Tiering Grosir*.
   - Saat Admin menambah/mengedit batch dengan tanggal kedaluwarsa dekat, `ExpiryAlertScheduler` belum langsung terpanggil (harus menunggu jadwal cron).
3. **Di Halaman Admin (`src/app/admin/products/page.tsx`):**
   - Belum ada panel UI untuk melihat daftar `ExpiryAlert` (`GET /api/inventory/alerts`), tombol pintas mengatur diskon batch dari alert, maupun tombol menyelesaikan alert (`PATCH /api/inventory/alerts/{id}/resolve`).
   - Belum ada modal UI untuk menambah/menghapus aturan harga grosir (`TieringPrice`).

Dengan perubahan ini, seluruh alur mulai dari **Input Batch Admin → Peringatan Expiry Alert → Katalog FrozenFood Deals → Keranjang Multi-Batch FEFO → Checkout B2C/B2B → Detail Pesanan → Nota PDF** menggunakan **satu sumber perhitungan harga yang sama (Single Source of Truth)** di Backend.

---

## 2. Bagian I — Perubahan Kode Backend (`frozenfood-api`)

### 1.1 `TieringPriceService.java` — Single Source of Truth Harga FEFO Deal, Clearance & Grosir

**File:** `src/main/java/com/lezzat/b2b2c/frozenfood/service/TieringPriceService.java`

#### Penjelasan Perubahan:
- Menambahkan metode `evaluateBatchPrice(Product, ProductBatch)` sebagai pusat perhitungan harga batch:
  - Jika sisa hari kedaluwarsa **$\le 7$ hari** dan belum ada `hargaOverride`: otomatis diskon minimal **50%** (`CLEARANCE`, label `"Clearance H-7 (-50%)"`).
  - Jika sisa hari kedaluwarsa **8 s/d 30 hari** dan belum ada `hargaOverride`/`diskon`: otomatis diskon **20%** (`FEFO_DEAL`, label `"FEFO Deal H-30 (-20%)"`).
  - Jika Admin menyetel `hargaOverride` atau `diskon` manual pada batch: dihitung sebagai `PROMO_BATCH`.
- Menambahkan metode `calculateOrderItemUnitPrice(...)` yang membandingkan harga deal batch FEFO dengan harga grosir `TieringPrice` (serta diskon mitra B2B 8%) dan **mengambil harga paling murah (paling menguntungkan pelanggan)**.
- Menyimpan harga aktual tiap batch (`deduction.rawUnitPrice()`) pada `OrderItem` agar rincian diskon per batch tidak kabur menjadi harga rata-rata (*blended*).

#### Kode Sebelum Diubah (Before):
```java
private static final int CLEARANCE_DAYS_THRESHOLD = 7;
private static final BigDecimal CLEARANCE_DISCOUNT_MULTIPLIER = new BigDecimal("0.50");
private static final BigDecimal B2B_REGULAR_DISCOUNT_MULTIPLIER = new BigDecimal("0.92");

public BigDecimal calculateFinalPrice(String tipeAkun, int quantity, ProductBatch batch) {
    BigDecimal hargaDasar = batch.getHargaOverride() != null
            ? batch.getHargaOverride()
            : batch.getProduct().getHargaEceran();

    LocalDate batasCuciGudang = LocalDate.now().plusDays(CLEARANCE_DAYS_THRESHOLD);
    if (!batch.getTanggalExp().isAfter(batasCuciGudang)) {
        log.warn("🚨 [CUCI GUDANG] Batch {} mendekati expired! Harga dipotong 50%.", batch.getIdProdBatch());
        return hargaDasar.multiply(CLEARANCE_DISCOUNT_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
    }

    if ("B2C".equalsIgnoreCase(tipeAkun)) {
        return hargaDasar.setScale(2, RoundingMode.HALF_UP);
    }

    if ("B2B".equalsIgnoreCase(tipeAkun)) {
        Optional<TieringPrice> tiering = tieringPriceRepository.findDynamicB2BPrice(
                batch.getProduct().getIdProduk(),
                quantity
        );
        if (tiering.isPresent()) {
            return tiering.get().getHargaPerUnit().setScale(2, RoundingMode.HALF_UP);
        }
        return hargaDasar.multiply(B2B_REGULAR_DISCOUNT_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
    }

    return hargaDasar.setScale(2, RoundingMode.HALF_UP);
}
```

#### Kode Sesudah Diubah (After):
```java
public static final int CLEARANCE_DAYS_THRESHOLD = 7;
public static final int NEAR_EXPIRY_DAYS_THRESHOLD = 30;
public static final BigDecimal CLEARANCE_DISCOUNT_PCT = new BigDecimal("50.00");
public static final BigDecimal NEAR_EXPIRY_DISCOUNT_PCT = new BigDecimal("20.00");
private static final BigDecimal B2B_REGULAR_DISCOUNT_MULTIPLIER = new BigDecimal("0.92");

public record BatchPriceEvaluation(
        BigDecimal hargaAsli,
        BigDecimal hargaEfektif,
        BigDecimal diskonPersen,
        long sisaHariExpired,
        boolean isDealBatch,
        String tipeDeal,
        String labelDeal
) {}

public record AppliedPriceResult(
        BigDecimal hargaAsli,
        BigDecimal hargaSatuanFinal,
        BigDecimal diskonPersen,
        String keteranganPromo
) {}

public BatchPriceEvaluation evaluateBatchPrice(Product product, ProductBatch batch) {
    BigDecimal hargaAsli = (product != null && product.getHargaEceran() != null)
            ? product.getHargaEceran().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    long daysLeft = batch.getTanggalExp() != null
            ? ChronoUnit.DAYS.between(LocalDate.now(), batch.getTanggalExp())
            : 999L;

    BigDecimal hargaDasar = (batch.getHargaOverride() != null
            && batch.getHargaOverride().compareTo(BigDecimal.ZERO) > 0)
            ? batch.getHargaOverride()
            : hargaAsli;

    BigDecimal configuredDiskon = (batch.getDiskon() != null
            && batch.getDiskon().compareTo(BigDecimal.ZERO) > 0)
            ? batch.getDiskon()
            : BigDecimal.ZERO;

    BigDecimal effectiveDiskon = configuredDiskon;
    if (batch.getHargaOverride() == null) {
        if (daysLeft <= CLEARANCE_DAYS_THRESHOLD
                && effectiveDiskon.compareTo(CLEARANCE_DISCOUNT_PCT) < 0) {
            effectiveDiskon = CLEARANCE_DISCOUNT_PCT;
        } else if (daysLeft <= NEAR_EXPIRY_DAYS_THRESHOLD
                && effectiveDiskon.compareTo(BigDecimal.ZERO) <= 0) {
            effectiveDiskon = NEAR_EXPIRY_DISCOUNT_PCT;
        }
    }

    BigDecimal hargaEfektif = effectiveDiskon.compareTo(BigDecimal.ZERO) > 0
            ? hargaDasar.multiply(BigDecimal.ONE.subtract(
                    effectiveDiskon.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                    .setScale(2, RoundingMode.HALF_UP)
            : hargaDasar.setScale(2, RoundingMode.HALF_UP);

    BigDecimal totalDiskonPct = (hargaAsli.compareTo(BigDecimal.ZERO) > 0 && hargaEfektif.compareTo(hargaAsli) < 0)
            ? hargaAsli.subtract(hargaEfektif).multiply(BigDecimal.valueOf(100)).divide(hargaAsli, 0, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    boolean isDeal = (daysLeft <= NEAR_EXPIRY_DAYS_THRESHOLD) || (hargaEfektif.compareTo(hargaAsli) < 0);

    String tipeDeal = null;
    String labelDeal = null;
    if (daysLeft <= CLEARANCE_DAYS_THRESHOLD) {
        tipeDeal = "CLEARANCE";
        labelDeal = "Clearance H-7 (-" + totalDiskonPct.stripTrailingZeros().toPlainString() + "%)";
    } else if (daysLeft <= NEAR_EXPIRY_DAYS_THRESHOLD) {
        tipeDeal = "FEFO_DEAL";
        labelDeal = "FEFO Deal H-30 (-" + totalDiskonPct.stripTrailingZeros().toPlainString() + "%)";
    } else if (hargaEfektif.compareTo(hargaAsli) < 0) {
        tipeDeal = "PROMO_BATCH";
        labelDeal = "Promo Batch (-" + totalDiskonPct.stripTrailingZeros().toPlainString() + "%)";
    }

    return new BatchPriceEvaluation(hargaAsli, hargaEfektif, totalDiskonPct, Math.max(0L, daysLeft), isDeal, tipeDeal, labelDeal);
}
```

---

### 1.2 `ExpiryAlertScheduler.java` — Otomasi Diskon H-30 (20%) & H-7 (50%) + Upgrade Alert

**File:** `src/main/java/com/lezzat/b2b2c/frozenfood/service/core/ExpiryAlertScheduler.java`

#### Penjelasan Perubahan:
- Sebelumnya, `ExpiryAlertScheduler` hanya menyetel `diskon = 50%` untuk batch H-7 (`isClearance`), sementara batch H-30 s/d H-8 dibiarkan tanpa diskon otomatis. Selain itu, jika sebuah batch awalnya masuk alert H-30 (`DARURAT`) lalu beberapa hari kemudian masuk H-7 (`CLEARANCE`), pesan alert lama tidak diperbarui karena dicegat oleh `existsByProductBatchAndIsResolvedFalse(batch)`.
- Sekarang:
  1. Batch **H-30 s/d H-8** otomatis diset `diskon = 20.00` (jika Admin belum mengisi `hargaOverride`/`diskon`).
  2. Batch **$\le$ H-7** otomatis diset `diskon = 50.00` (jika diskon sebelumnya $< 50\%$).
  3. Jika batch sudah memiliki alert `DARURAT` yang belum diselesaikan dan kini memasuki periode H-7, pesan alert otomatis **di-upgrade menjadi `CLEARANCE 50%`**.

#### Kode Sebelum Diubah (Before):
```java
for (ProductBatch batch : criticalBatches) {
    boolean isClearance = !batch.getTanggalExp().isAfter(clearanceDate);
    if (isClearance && batch.getHargaOverride() == null
            && (batch.getDiskon() == null || batch.getDiskon().compareTo(CLEARANCE_DISCOUNT) != 0)) {
        batch.setDiskon(CLEARANCE_DISCOUNT);
        productBatchRepository.save(batch);
    }

    if (!expiryAlertRepository.existsByProductBatchAndIsResolvedFalse(batch)) {
        String prefix = isClearance ? "CLEARANCE 50%: " : "DARURAT: ";
        ExpiryAlert alert = ExpiryAlert.builder()
                .productBatch(batch)
                .pesan(prefix + "Produk " + batch.getProduct().getNamaProduk() + " (Batch: " + batch.getIdProdBatch() + ") kedaluwarsa pada " + batch.getTanggalExp())
                .build();
        
        expiryAlertRepository.save(alert);
        alertCount++;
    }
}
```

#### Kode Sesudah Diubah (After):
```java
for (ProductBatch batch : criticalBatches) {
    boolean isClearance = !batch.getTanggalExp().isAfter(clearanceDate);
    if (isClearance) {
        if (batch.getHargaOverride() == null
                && (batch.getDiskon() == null || batch.getDiskon().compareTo(CLEARANCE_DISCOUNT) < 0)) {
            batch.setDiskon(CLEARANCE_DISCOUNT);
            productBatchRepository.save(batch);
        }
    } else {
        // Batch H-30 s/d H-8 otomatis dapat diskon FEFO Deal 20% jika belum diset Admin
        if (batch.getHargaOverride() == null
                && (batch.getDiskon() == null || batch.getDiskon().compareTo(BigDecimal.ZERO) <= 0)) {
            batch.setDiskon(NEAR_EXPIRY_DISCOUNT);
            productBatchRepository.save(batch);
        }
    }

    BigDecimal effectiveDisc = batch.getDiskon() != null
            ? batch.getDiskon()
            : (isClearance ? CLEARANCE_DISCOUNT : NEAR_EXPIRY_DISCOUNT);
    String prefix = isClearance
            ? "CLEARANCE " + effectiveDisc.stripTrailingZeros().toPlainString() + "%: "
            : "DARURAT (FEFO DEAL " + effectiveDisc.stripTrailingZeros().toPlainString() + "%): ";
    String expectedMessage = prefix + "Produk " + batch.getProduct().getNamaProduk()
            + " (Batch: " + batch.getIdProdBatch() + ") kedaluwarsa pada " + batch.getTanggalExp();

    var existingAlertOpt = expiryAlertRepository
            .findFirstByProductBatchAndIsResolvedFalseOrderByCreatedAtDesc(batch);

    if (existingAlertOpt.isEmpty()) {
        ExpiryAlert alert = ExpiryAlert.builder()
                .productBatch(batch)
                .pesan(expectedMessage)
                .build();
        expiryAlertRepository.save(alert);
        alertCount++;
    } else if (isClearance && !existingAlertOpt.get().getPesan().startsWith("CLEARANCE")) {
        ExpiryAlert existingAlert = existingAlertOpt.get();
        existingAlert.setPesan(expectedMessage);
        expiryAlertRepository.save(existingAlert);
    }
}
```

---

### 1.3 `ProductBatchService.java` — Trigger Instan Expiry Alert Saat Tambah/Edit Batch

**File:** `src/main/java/com/lezzat/b2b2c/frozenfood/service/ProductBatchService.java`

#### Penjelasan Perubahan:
- Menginjeksi `ExpiryAlertScheduler` ke dalam `ProductBatchService` dan memanggil `expiryAlertScheduler.scanAndGenerateExpiryAlerts()` setiap kali Admin menambah batch baru (`addNewBatch`), mengedit batch (`updateBatch`), atau mengubah harga/diskon batch (`updateBatchPricing`).
- Hasilnya: begitu Admin menginput batch dengan tanggal kedaluwarsa $\le 30$ hari di halaman Admin, **detik itu juga** diskon dan notifikasi Expiry Alert langsung muncul di UI Admin dan Katalog Beranda.

#### Kode Sebelum Diubah (Before):
```java
batch.setHargaOverride(req.hargaOverride());
batch.setDiskon(req.diskon());

return mapToResponse(productBatchRepository.save(batch));
```

#### Kode Sesudah Diubah (After):
```java
batch.setHargaOverride(req.hargaOverride());
batch.setDiskon(req.diskon());

ProductBatch saved = productBatchRepository.save(batch);
expiryAlertScheduler.scanAndGenerateExpiryAlerts();
return mapToResponse(saved);
```

---

### 1.4 `InventoryController.java` & `ExpiryAlertRepository.java` — Ekspos Data Harga Batch pada Alert Admin

**File:**
- `src/main/java/com/lezzat/b2b2c/frozenfood/controller/InventoryController.java`
- `src/main/java/com/lezzat/b2b2c/frozenfood/repository/ExpiryAlertRepository.java`

#### Penjelasan Perubahan:
- Menambahkan field `hargaEceran`, `hargaOverride`, dan `diskon` pada `ExpiryAlertResponse` di `InventoryController.java` agar panel Expiry Alert di Admin dapat langsung menampilkan persentase diskon aktif dan mengisi nilai awal saat tombol **"Atur Harga / Diskon Batch"** diklik.

#### Kode Sebelum Diubah (Before):
```java
public record ExpiryAlertResponse(
        UUID idAlert,
        String idProdBatch,
        String idProduk,
        String namaProduk,
        LocalDate tanggalExp,
        Integer stokOnline,
        String pesan,
        Boolean isResolved,
        LocalDateTime createdAt
) {}
```

#### Kode Sesudah Diubah (After):
```java
public record ExpiryAlertResponse(
        UUID idAlert,
        String idProdBatch,
        String idProduk,
        String namaProduk,
        LocalDate tanggalExp,
        Integer stokOnline,
        java.math.BigDecimal hargaEceran,
        java.math.BigDecimal hargaOverride,
        java.math.BigDecimal diskon,
        String pesan,
        Boolean isResolved,
        LocalDateTime createdAt
) {}
```

---

### 1.5 `ProductCatalogResponse.java` & `ProductService.java` — Ekspos Rincian Deal & `activeBatches` ke Katalog

**File:**
- `src/main/java/com/lezzat/b2b2c/frozenfood/dto/response/ProductCatalogResponse.java`
- `src/main/java/com/lezzat/b2b2c/frozenfood/service/ProductService.java`

#### Penjelasan Perubahan:
- Sebelumnya, `ProductService` hanya memeriksa apakah ada batch $\le 7$ hari (`CLEARANCE_DAYS_THRESHOLD = 7`). Batch H-30 maupun batch yang diberi diskon manual oleh Admin tidak muncul sebagai promo di katalog, dan Frontend tidak mengetahui rincian stok per batch FEFO (`activeBatches`).
- Sekarang, `ProductService` memanggil `tieringPriceService.evaluateBatchPrice(product, batch)` untuk setiap batch aktif (diurutkan berdasarkan `tanggalExp` terdekat / FEFO) dan mengirimkan:
  - `hargaSetelahDiskon`, `diskonPersen`, `sisaHariExpired`, `tipeDeal` (`CLEARANCE` / `FEFO_DEAL` / `PROMO_BATCH`), `labelDeal`
  - `activeBatches`: daftar batch aktif berurutan FEFO lengkap dengan `stokOnline`, `hargaEfektif`, `diskonPersen`, dan `labelDeal`.

#### Kode Sebelum Diubah (Before):
```java
if (!batch.getTanggalExp().isAfter(clearanceCutoff)) {
    stokClearance += batch.getStokOnline();
    if (hargaClearance == null) {
        hargaClearance = batch.getHargaOverride() != null
                ? batch.getHargaOverride()
                : product.getHargaEceran().multiply(CLEARANCE_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
    }
}
```

#### Kode Sesudah Diubah (After):
```java
TieringPriceService.BatchPriceEvaluation eval = tieringPriceService.evaluateBatchPrice(product, batch);

batchCatalogItems.add(new ProductCatalogResponse.BatchCatalogItem(
        batch.getIdProdBatch(),
        batch.getStokOnline(),
        batch.getTanggalExp(),
        eval.sisaHariExpired(),
        eval.hargaEfektif(),
        eval.diskonPersen(),
        eval.isDealBatch(),
        eval.labelDeal()
));

if (eval.isDealBatch()) {
    stokClearance += batch.getStokOnline();
    if (hargaClearance == null) {
        hargaClearance = eval.hargaEfektif();
        diskonPersen = eval.diskonPersen();
        sisaHariExpired = eval.sisaHariExpired();
        tipeDeal = eval.tipeDeal();
        labelDeal = eval.labelDeal();
        nearestExpDate = batch.getTanggalExp();
    }
}
```

---

### 1.6 `OrderItemResponse.java`, `OrderB2CService.java` & `OrderService.java` — Harga Deal & Tiering Saat Checkout + Rincian Promo

**File:**
- `src/main/java/com/lezzat/b2b2c/frozenfood/dto/response/OrderItemResponse.java`
- `src/main/java/com/lezzat/b2b2c/frozenfood/service/OrderB2CService.java`
- `src/main/java/com/lezzat/b2b2c/frozenfood/service/OrderService.java`

#### Penjelasan Perubahan:
- `OrderItemResponse` kini menyertakan `hargaAsli`, `diskonPersen`, dan `keteranganPromo` di samping `hargaSatuanSnapshot` dan `subtotalItem`.
- `OrderB2CService` kini memanggil `tieringPriceService.calculateOrderItemUnitPrice(product, batch, itemRequest.quantity(), tipeAkun)` sehingga pembeli retail (B2C) yang membeli produk *FrozenFood Deals* atau membeli dalam jumlah grosir (memenuhi `minQty` Tiering) otomatis mendapatkan potongan harga yang sah di database.
- `OrderService` (B2B) menyimpan harga aktual per batch (`deduction.rawUnitPrice()`) ke `OrderItem` sehingga jelas batch mana yang mendapat harga Clearance 50% dan batch mana yang mendapat harga Tiering Grosir.

#### Kode Sebelum Diubah (Before — `OrderB2CService.java`):
```java
int diambilDariBatchIni = Math.min(batch.getStokOnline(), sisaDibutuhkan);
BigDecimal hargaEfektif = hitungHargaEfektif(product, batch);
BigDecimal subtotal = hargaEfektif.multiply(BigDecimal.valueOf(diambilDariBatchIni));
```

#### Kode Sesudah Diubah (After — `OrderB2CService.java`):
```java
int diambilDariBatchIni = Math.min(batch.getStokOnline(), sisaDibutuhkan);
TieringPriceService.AppliedPriceResult applied = tieringPriceService
        .calculateOrderItemUnitPrice(product, batch, itemRequest.quantity(), tipeAkun);
BigDecimal hargaEfektif = applied.hargaSatuanFinal();
BigDecimal subtotal = hargaEfektif.multiply(BigDecimal.valueOf(diambilDariBatchIni))
        .setScale(2, RoundingMode.HALF_UP);
```

---

### 1.7 `NotaPdfService.java` — Tabel Harga Normal vs Harga Deal/Tier & Total Hemat di Nota PDF

**File:** `src/main/java/com/lezzat/b2b2c/frozenfood/service/NotaPdfService.java`

#### Penjelasan Perubahan:
- Memperbarui tabel PDF 4 kolom menjadi **5 kolom** (`Produk & Batch FEFO`, `Qty`, `Harga Normal`, `Harga Deal/Tier`, `Subtotal`).
- Menampilkan kode batch FEFO dan label promo (`[Clearance H-7 (-50%)]`, `[FEFO Deal H-30 (-20%)]`, atau `[Grosir/Diskon -X%]`) di bawah nama produk.
- Menambahkan baris **Total Harga Normal** dan **Total Hemat (Diskon / Tiering)** pada ringkasan biaya Nota PDF.

#### Kode Sebelum Diubah (Before):
```java
PdfPTable tabel = new PdfPTable(new float[]{4f, 1f, 2.2f, 2.2f});
header(tabel, "Produk", Element.ALIGN_LEFT);
header(tabel, "Qty", Element.ALIGN_CENTER);
header(tabel, "Harga Satuan", Element.ALIGN_RIGHT);
header(tabel, "Subtotal", Element.ALIGN_RIGHT);
```

#### Kode Sesudah Diubah (After):
```java
PdfPTable tabel = new PdfPTable(new float[]{3.6f, 0.8f, 1.8f, 2.0f, 2.0f});
header(tabel, "Produk & Batch FEFO", Element.ALIGN_LEFT);
header(tabel, "Qty", Element.ALIGN_CENTER);
header(tabel, "Harga Normal", Element.ALIGN_RIGHT);
header(tabel, "Harga Deal/Tier", Element.ALIGN_RIGHT);
header(tabel, "Subtotal", Element.ALIGN_RIGHT);
...
if (totalHemat.compareTo(BigDecimal.ZERO) > 0) {
    barisBiaya(ringkas, "Total Harga Normal", rupiah(totalHargaNormal), F_ISI);
    barisBiaya(ringkas, "Total Hemat (Diskon / Tiering)", "- " + rupiah(totalHemat), F_LABEL);
}
```

---

### 1.8 `application.yml` — Perbaikan Key Konfigurasi Konversi Berat & Volume

**File:** `src/main/resources/application.yml`

#### Penjelasan Perubahan:
- Memperbaiki salah ketik key YAML `u unit:` menjadi `unit:` di bawah `lezzat.delivery` agar properti `weight-to-kg: 0.001` dan `volume-to-liter: 0.001` terikat dengan benar ke `DeliveryProperties`.

```yaml
# Sebelum (Before):
    u unit: { weight-to-kg: 0.001, volume-to-liter: 0.001 }

# Sesudah (After):
    unit: { weight-to-kg: 0.001, volume-to-liter: 0.001 }
```

---

## 3. Bagian II — Perubahan Kode Frontend (`lezzat-frontend`)

### 2.1 `src/types/api.ts` — Kontrak TypeScript Baru untuk Deals, Batch Aktif, Tiering & Expiry Alert

**File:** `src/types/api.ts`

#### Penjelasan Perubahan:
- Menambahkan interface `TieringPriceItem`, `BatchCatalogItem`, dan `ExpiryAlertResponse`.
- Memperluas `ProductCatalogResponse` dengan field `isClearance`, `stokClearance`, `hargaClearance`, `hargaSetelahDiskon`, `diskonPersen`, `sisaHariExpired`, `tipeDeal`, `labelDeal`, `nearestExpDate`, `tieringPrices`, dan `activeBatches`.
- Memperluas `OrderItemResponse` dengan `hargaAsli`, `diskonPersen`, dan `keteranganPromo`.

---

### 2.2 `src/app/admin/products/page.tsx` — Panel Expiry Alert Admin & Modal CRUD Tiering Grosir

**File:** `src/app/admin/products/page.tsx`

#### Penjelasan Perubahan:
1. **Panel Peringatan Kedaluwarsa (`ExpiryAlert`):**
   - Memanggil `GET /api/inventory/alerts?unresolvedOnly=...` menggunakan React Query.
   - Menampilkan badge status (`CLEARANCE H-7 (50%)` berwarna merah atau `DARURAT H-30 (FEFO DEAL)` berwarna oranye), kode batch, sisa stok online, tanggal kedaluwarsa, serta diskon/override aktif.
   - Menyediakan tombol **"Atur Harga / Diskon Batch"** (`handleEditBatchFromAlert`) yang langsung membuka modal edit batch untuk batch tersebut.
   - Menyediakan tombol **"Tandai Selesai"** (`PATCH /api/inventory/alerts/{idAlert}/resolve`).
2. **Modal Kelola Harga Grosir Bertingkat (`TieringPrice`):**
   - Menambahkan tombol **"Tiering"** pada setiap baris produk di tabel Admin.
   - Membuka modal untuk melihat daftar aturan grosir aktif, menambah aturan baru (`POST /api/products/{idProduk}/tiering`), dan menghapus aturan (`DELETE /api/products/tiering/{idTiering}`).
3. **Indikator Deal & Tiering di Tabel Produk Admin:**
   - Pada kolom harga produk di tabel Admin, menampilkan badge `Deal: Rp ... (-X%)` jika produk sedang memiliki batch promo aktif, serta jumlah aturan grosir (`X Aturan Grosir (Tiering)`).

#### Cuplikan Kode Baru (`src/app/admin/products/page.tsx`):
```tsx
const {
  data: expiryAlerts = [],
  isLoading: isLoadingAlerts,
  refetch: refetchAlerts,
} = useQuery<ExpiryAlertResponse[]>({
  queryKey: ['admin-expiry-alerts', showResolvedAlerts],
  queryFn: async () => {
    const res = await api.get<ExpiryAlertResponse[]>(
      '/api/inventory/alerts',
      { params: { unresolvedOnly: !showResolvedAlerts } }
    );
    return res.data;
  },
});

const handleResolveAlert = async (alertItem: ExpiryAlertResponse) => {
  setResolvingAlertId(alertItem.idAlert);
  try {
    await api.patch(`/api/inventory/alerts/${alertItem.idAlert}/resolve`);
    await refetchAlerts();
  } finally {
    setResolvingAlertId(null);
  }
};
```

---

### 2.3 `src/app/page.tsx` — Banner "FrozenFood Deals" Murni dari Data Backend (Tanpa Angka Dummy)

**File:** `src/app/page.tsx`

#### Penjelasan Perubahan:
- **Sebelum:** Jika produk di katalog belum memiliki field diskon, `fefoDealProducts` mengambil 4 produk stok paling sedikit lalu mengarang angka `daysLeft` (`(((idx + 1) * 2) % 5) + 3`) dan mengarang diskon 15% / 20% di Frontend, serta memodifikasi `product.hargaEceran` secara lokal saat tombol `+ Ambil Deal` diklik.
- **Sesudah:** `fefoDealProducts` memfilter **murni produk yang benar-benar berstatus deal dari Backend** (`p.isClearance === true && dealPrice < p.hargaEceran`), mengurutkannya berdasarkan `sisaHariExpired` terdekat, menampilkan stok promo (`stokClearance`), dan saat dimasukkan ke keranjang (`addItem(product, 1)`), keranjang otomatis menghitung alokasi stok promo vs stok normal menggunakan data `activeBatches` dari backend.

#### Kode Sebelum Diubah (Before):
```tsx
const fefoDealProducts = useMemo(() => {
  const available = activeProducts.filter((p) => getProductStock(p) > 0);
  const sorted = [...available].sort((a, b) => getProductStock(a) - getProductStock(b));

  return sorted.slice(0, 4).map((product, idx) => {
    const daysLeft = apiDaysLeft ?? (((idx + 1) * 2) % 5) + 3;
    const discountPct = apiDiscountPct ?? (idx % 2 === 0 ? 20 : 15);
    const discountedPrice = Math.round((originalPrice * (100 - discountPct)) / 100);
    ...
  });
}, [activeProducts]);
```

#### Kode Sesudah Diubah (After):
```tsx
const fefoDealProducts = useMemo(() => {
  const dealCandidates = activeProducts.filter((p) => {
    const dealPrice = p.hargaClearance ?? p.hargaSetelahDiskon ?? null;
    return (
      p.isClearance === true &&
      getProductStock(p) > 0 &&
      typeof dealPrice === 'number' &&
      dealPrice > 0 &&
      dealPrice < p.hargaEceran
    );
  });

  const sorted = [...dealCandidates].sort((a, b) => {
    const daysA = typeof a.sisaHariExpired === 'number' ? a.sisaHariExpired : 999;
    const daysB = typeof b.sisaHariExpired === 'number' ? b.sisaHariExpired : 999;
    return daysA - daysB;
  });

  return sorted.slice(0, 8).map((product) => {
    const stock =
      typeof product.stokClearance === 'number' && product.stokClearance > 0
        ? product.stokClearance
        : getProductStock(product);
    const daysLeft = typeof product.sisaHariExpired === 'number' ? product.sisaHariExpired : 0;
    const originalPrice = product.hargaEceran;
    const discountedPrice = product.hargaClearance ?? product.hargaSetelahDiskon ?? product.hargaEceran;
    const discountPct = typeof product.diskonPersen === 'number' ? product.diskonPersen : 0;
    const savedAmount = Math.max(0, originalPrice - discountedPrice);
    return { product, stock, daysLeft, discountPct, originalPrice, discountedPrice, savedAmount };
  });
}, [activeProducts]);
```

---

### 2.4 `src/components/shop/product-card.tsx` & `src/app/products/[id]/page.tsx` — Label Promo, Harga Coret & Tabel Tiering

**File:**
- `src/components/shop/product-card.tsx`
- `src/app/products/[id]/page.tsx`

#### Penjelasan Perubahan:
- Fungsi `extractProductPromo(product)` kini membaca `product.isClearance`, `product.hargaClearance`, `product.diskonPersen`, dan `product.labelDeal` langsung dari response API Backend.
- Kartu produk (`ProductCard`) dan halaman detail produk menampilkan harga setelah diskon (`finalPrice`) berwarna oranye `#F75C03` beserta harga asli yang dicoret (`originalPrice`) dan badge label deal (`Clearance H-7 (-50%)` / `FEFO Deal H-30 (-20%)`).
- Fungsi `extractB2BTiers(product)` di `src/app/products/[id]/page.tsx` kini membaca `product.tieringPrices` (`minQty`, `maxQty`, `hargaPerUnit`) langsung dari backend dan menampilkan tabel harga grosir untuk semua pengunjung, serta menghitung estimasi subtotal di halaman detail produk menggunakan `evaluateCartItemPricing(product, qty, user?.tipeAkun)`.

---

### 2.5 `src/lib/utils.ts` & `src/store/cart-store.ts` — Kalkulator Harga Multi-Batch FEFO (`evaluateCartItemPricing`)

**File:**
- `src/lib/utils.ts`
- `src/store/cart-store.ts`

#### Penjelasan Perubahan:
- **Sebelum:** `cart-store.ts` hanya mengalikan `product.hargaEceran * item.qty` pada `getSubtotal()`. Akibatnya, diskon batch FEFO maupun harga grosir Tiering B2B tidak pernah terlihat di keranjang atau ringkasan checkout.
- **Sesudah:**
  - Menambahkan fungsi `evaluateCartItemPricing(product, qty, tipeAkun)` di `src/lib/utils.ts` yang mensimulasikan alokasi stok per batch FEFO (`product.activeBatches`) persis seperti algoritma Backend:
    - Misalnya Batch 1 (Clearance 50%, Rp 20.000) punya stok 2 pcs, dan Batch 2 (Normal, Rp 40.000) punya stok 10 pcs. Jika user membeli **5 pcs**, fungsi ini otomatis membagi menjadi **2x Rp 20.000 (Clearance)** + **3x Rp 40.000 (Normal)** = **Rp 160.000** (Hemat Rp 40.000).
    - Jika kuantitas pembelian memenuhi syarat `tieringPrices` (atau diskon mitra B2B) yang lebih murah daripada harga batch, otomatis menggunakan harga Tiering tersebut.
  - `useCartStore` kini menyediakan `getSubtotal(tipeAkun)`, `getOriginalSubtotal()`, dan `getTotalSaved(tipeAkun)`.

---

### 2.6 `src/components/shop/cart-panel.tsx`, `src/app/checkout/page.tsx` & `src/app/orders/[id]/page.tsx` — Sinkronisasi Rincian Diskon & Penghematan

**File:**
- `src/components/shop/cart-panel.tsx`
- `src/app/checkout/page.tsx`
- `src/app/orders/[id]/page.tsx`

#### Penjelasan Perubahan:
1. **Panel Keranjang (`cart-panel.tsx`):** Menampilkan harga coret vs harga efektif per item, badge promo (`Clearance H-7 (-50%)` / `Harga Grosir Tier ...`), serta baris **Harga Normal** dan **Hemat Diskon / Tier** di bagian bawah keranjang.
2. **Halaman Checkout (`checkout/page.tsx`):** Menampilkan rincian pembagian batch (`pricing.lines`) jika pembelian melintasi lebih dari satu batch FEFO dengan harga berbeda, serta menampilkan **Total Harga Normal** dan **Total Hemat (Diskon / Tiering)** yang 100% cocok dengan hasil tagihan `POST /api/orders`.
3. **Halaman Detail Pesanan (`orders/[id]/page.tsx`):** Membaca `item.hargaAsli`, `item.hargaSatuanSnapshot`, dan `item.keteranganPromo` dari backend untuk menampilkan badge promo di tiap baris item, harga asli yang dicoret, kode batch FEFO, serta rangkuman **Total Hemat (Diskon FEFO / Tiering)**.
