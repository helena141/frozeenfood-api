# Dokumentasi Perubahan Backend — Kesiapan Integrasi Next.js (Multi-Repo)

> [!TIP]
> Seluruh perubahan telah dikompilasi dan diverifikasi menggunakan `./mvnw test-compile` dengan status **BUILD SUCCESS**. Seluruh komentar dan arsitektur yang sudah ada tetap dipertahankan.

---

## Ringkasan File yang Diubah

| No | File | Kategori Perubahan | Tujuan Integrasi Next.js |
| :--- | :--- | :--- | :--- |
| 1 | [`SecurityConfig.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/security/SecurityConfig.java) | Security & CORS | Mengizinkan *request* lintas domain dari `http://localhost:3000` (Next.js) dan membuka webhook `/api/payments/callback`. |
| 2 | [`AuthResponse.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/dto/response/AuthResponse.java) & [`AuthService.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/service/AuthService.java) | DTO & Auth | Menyertakan `idUser` (UUID) saat *login* dan *register* agar frontend B2C dapat mengirim `idUser` ke `POST /api/orders/retail`. |
| 3 | [`OrderController.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/controller/OrderController.java), [`OrderService.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/service/OrderService.java), & [`OrderRepository.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/repository/OrderRepository.java) | Order (B2B/B2C) | Menyeragamkan respons `POST /api/orders/checkout` menjadi [`OrderResponse`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/dto/response/OrderResponse.java) serta menambah endpoint `GET /api/orders/my` & `GET /api/orders/{idOrder}`. |
| 4 | [`ProductBatchController.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/controller/ProductBatchController.java), [`ProductBatchService.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/service/ProductBatchService.java), & [`ProductBatchRepository.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/repository/ProductBatchRepository.java) | Inventory FEFO | Menambahkan endpoint `GET /api/inventory/batches` (dengan filter opsional `?productId=...`) untuk menampilkan tabel batch FEFO di Dashboard Admin. |

---

## Rincian Teknis Perubahan

### 1. Konfigurasi CORS & Webhook Pembayaran ([`SecurityConfig.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/security/SecurityConfig.java#L48-L91))
* Mengaktifkan `.cors(Customizer.withDefaults())` pada [`filterChain`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/security/SecurityConfig.java#L48-L74).
* Menambahkan bean [`corsConfigurationSource()`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/security/SecurityConfig.java#L77-L90) yang:
  * Mengizinkan origin `http://localhost:3000` dan `http://127.0.0.1:3000`.
  * Mengizinkan method `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
  * Mengekspos header `Content-Disposition` (penting agar Next.js dapat membaca nama file saat mengunduh PDF Nota dari `/api/orders/{idOrder}/nota`) dan `Authorization`.
* Menambahkan `.requestMatchers("/api/payments/callback").permitAll()` sesuai desain webhook pada [`PaymentController.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/controller/PaymentController.java#L29-L33).

### 2. Penambahan `idUser` pada Respons Autentikasi ([`AuthResponse.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/dto/response/AuthResponse.java#L14-L21) & [`AuthService.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/service/AuthService.java#L57-L85))
* Menambahkan properti `UUID idUser` pada [`AuthResponse`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/dto/response/AuthResponse.java#L16).
* Menyertakan `.idUser(customer.getIdUser())` pada method [`register`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/service/AuthService.java#L58) dan [`login`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/service/AuthService.java#L80).
* **Contoh JSON Response `/api/auth/login` & `/api/auth/register` Sekarang:**
  ```json
  {
    "idUser": "348bb928-30f4-4492-8c99-33ec3f5553e5",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "nama": "Budi Santoso",
    "email": "budi@example.com",
    "tipeAkun": "B2C"
  }
  ```

### 3. Standarisasi DTO `OrderResponse` & Endpoint Riwayat Pesanan ([`OrderController.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/controller/OrderController.java#L29-L46))
* Mengubah return type [`OrderService.createOrder()`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/service/OrderService.java#L35-L78) dari entitas JPA `Order` menjadi DTO [`OrderResponse`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/dto/response/OrderResponse.java). Hal ini mencegah error *LazyInitializationException* pada relasi `Customer` dan memastikan rincian batch (`idProdBatch`, `idProduk`, `namaProduk`) terkirim ke frontend karena di entitas `OrderItem` relasi `productBatch` diberi anotasi `@JsonIgnore`.
* Menambahkan 2 endpoint baca di [`OrderController`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/controller/OrderController.java#L38-L46):
  * `GET /api/orders/my`: Mengambil daftar pesanan milik pelanggan yang sedang login (diurutkan dari yang terbaru) menggunakan query `JOIN FETCH` di [`OrderRepository.findByCustomerIdUserWithItems`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/repository/OrderRepository.java#L26-L27).
  * `GET /api/orders/{idOrder}`: Mengambil detail satu pesanan beserta daftar item per batch-nya.

### 4. Endpoint Daftar Batch Inventori FEFO ([`ProductBatchController.java`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/controller/ProductBatchController.java#L27-L31))
* Menambahkan `GET /api/inventory/batches` di [`ProductBatchController`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/controller/ProductBatchController.java#L27-L31) yang memanggil [`ProductBatchService.getBatches(productId)`](file:///home/reno/frozenfood-api/src/main/java/com/lezzat/b2b2c/frozenfood/service/ProductBatchService.java#L41-L50).
* Mendukung pemanggilan seluruh batch (`GET /api/inventory/batches`) maupun difilter per produk (`GET /api/inventory/batches?productId=<uuid>`), diurutkan berdasarkan `tanggalExp ASC` sesuai prinsip **FEFO**.
