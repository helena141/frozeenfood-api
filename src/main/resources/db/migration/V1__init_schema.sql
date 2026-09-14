-- ========================================================================================
-- MIGRATION FILE: V1__init_schema.sql
-- ARCHITECT: Big Moreno
-- ENGINE: PostgreSQL 15 (Flyway Ready)
-- DOMAIN: Lezzat Frozen Food (B2B & B2C)
-- CORE LOGIC: Auto-Tiering Price (B2B) & FEFO Inventory (Batch-based Expiry)
-- ========================================================================================

-- ----------------------------------------------------------------------------------------
-- [0] AUTOMATION: UPDATED_AT TRIGGER
-- Memastikan kolom updated_at otomatis disuntik oleh mesin PostgreSQL setiap ada operasi UPDATE.
-- ----------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trigger_set_tiimestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ----------------------------------------------------------------------------------------
-- [1] TABLE: customers
-- Master data pengguna.
-- ----------------------------------------------------------------------------------------
CREATE TABLE customers (
    id_user UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    no_telp VARCHAR(25) NOT NULL,
    alamat TEXT NOT NULL,
    tipe_akun VARCHAR(20) NOT NULL CHECK (tipe_akun IN ('B2C', 'B2B', 'ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE TRIGGER set_timestamp_customers
BEFORE UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ----------------------------------------------------------------------------------------
-- [2] TABLE: products
-- Master data katalog produk beku.
-- ----------------------------------------------------------------------------------------
CREATE TABLE products (
    id_produk UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama_produk VARCHAR(200) NOT NULL,
    deskripsi_produk TEXT,
    kategori VARCHAR(100) NOT NULL,
    harga_eceran NUMERIC(15, 2) NOT NULL CHECK (harga_eceran >= 0),
    berat DECIMAL(10, 2) NOT NULL CHECK (berat > 0),
    volume DECIMAL(10, 2),
    gambar TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE TRIGGER set_timestamp_products
BEFORE UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ----------------------------------------------------------------------------------------
-- [3] TABLE: tiering_prices
-- Logika B2B: Harga khusus berdasarkan kuantitas beli.
-- ----------------------------------------------------------------------------------------
CREATE TABLE tiering_prices (
    id_tiering UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_produk UUID NOT NULL,
    min_qty INT NOT NULL CHECK (min_qty > 0),
    max_qty INT NOT NULL CHECK (max_qty >= min_qty),
    harga_per_unit NUMERIC(15, 2) NOT NULL CHECK (harga_per_unit >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_tiering_produk FOREIGN KEY (id_produk) 
        REFERENCES products (id_produk) ON DELETE RESTRICT
);

CREATE TRIGGER set_timestamp_tiering_prices
BEFORE UPDATE ON tiering_prices
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ----------------------------------------------------------------------------------------
-- [4] TABLE: produk_batches
-- Jantung logika FEFO. Stok dipisah berdasarkan tanggal kedaluwarsa.
-- ----------------------------------------------------------------------------------------
CREATE TABLE produk_batches (
    id_prod_batch UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_produk UUID NOT NULL,
    stok_awal INT NOT NULL CHECK (stok_awal >= 0),
    stok_online INT NOT NULL CHECK (stok_online >= 0 AND stok_online <= stok_awal),
    tanggal_exp DATE NOT NULL,
    harga_override NUMERIC(15, 2) CHECK (harga_override >= 0),
    diskon NUMERIC(5, 2) CHECK (diskon >= 0 AND diskon <= 100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_batch_produk FOREIGN KEY (id_produk) 
        REFERENCES products (id_produk) ON DELETE RESTRICT
);

CREATE TRIGGER set_timestamp_produk_batches
BEFORE UPDATE ON produk_batches
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ----------------------------------------------------------------------------------------
-- [5] TABLE: expiry_alerts
-- Sistem notifikasi dini sebelum batch kedaluwarsa.
-- ----------------------------------------------------------------------------------------
CREATE TABLE expiry_alerts (
    id_expiry UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_prod_batch UUID NOT NULL,
    hari_sebelum INT NOT NULL CHECK (hari_sebelum > 0),
    send_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_batch FOREIGN KEY (id_prod_batch) 
        REFERENCES produk_batches (id_prod_batch) ON DELETE CASCADE
);

CREATE TRIGGER set_timestamp_expiry_alerts
BEFORE UPDATE ON expiry_alerts
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ----------------------------------------------------------------------------------------
-- [6] TABLE: orders
-- Header transaksi pelanggan.
-- ----------------------------------------------------------------------------------------
CREATE TABLE orders (
    id_order UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_user UUID NOT NULL,
    tipe_order VARCHAR(50) NOT NULL CHECK (tipe_order IN ('B2C', 'B2B')),
    status VARCHAR(50) NOT NULL,
    ongkir NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (ongkir >= 0),
    catatan TEXT,
    total NUMERIC(15, 2) NOT NULL CHECK (total >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_order_user FOREIGN KEY (id_user) 
        REFERENCES customers (id_user) ON DELETE RESTRICT
);

CREATE TRIGGER set_timestamp_orders
BEFORE UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ----------------------------------------------------------------------------------------
-- [7] TABLE: order_items
-- Detail transaksi (mengikat langsung ke id_prod_batch demi akurasi potong stok FEFO).
-- ----------------------------------------------------------------------------------------
CREATE TABLE order_items (
    id_order_item UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_order UUID NOT NULL,
    id_prod_batch UUID NOT NULL,
    qty INT NOT NULL CHECK (qty > 0),
    harga_satuan_snapshot NUMERIC(15, 2) NOT NULL CHECK (harga_satuan_snapshot >= 0),
    subtotal_item NUMERIC(15, 2) NOT NULL CHECK (subtotal_item >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_order FOREIGN KEY (id_order) 
        REFERENCES orders (id_order) ON DELETE RESTRICT,
    CONSTRAINT fk_item_order FOREIGN KEY (id_order) 
    REFERENCES orders (id_order) ON DELETE CASCADE,
);

CREATE TRIGGER set_timestamp_order_items
BEFORE UPDATE ON order_items
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ----------------------------------------------------------------------------------------
-- [8] TABLE: payments
-- Rekaman riwayat pembayaran (Payment Gateway integration).
-- ----------------------------------------------------------------------------------------
CREATE TABLE payments (
    id_payment UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_order UUID NOT NULL UNIQUE,
    metode_pay VARCHAR(100) NOT NULL,
    gateway_ref VARCHAR(255),
    jumlah NUMERIC(15, 2) NOT NULL CHECK (jumlah > 0),
    status VARCHAR(50) NOT NULL,
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_order FOREIGN KEY (id_order) 
        REFERENCES orders (id_order) ON DELETE RESTRICT
);

CREATE TRIGGER set_timestamp_payments
BEFORE UPDATE ON payments
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ----------------------------------------------------------------------------------------
-- [9] TABLE: deliveries
-- Rekaman resi dan pengiriman.
-- ----------------------------------------------------------------------------------------
CREATE TABLE deliveries (
    id_deliv UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_order UUID NOT NULL UNIQUE,
    metode_deliv VARCHAR(100) NOT NULL,
    alamat_tujuan TEXT NOT NULL,
    jarak DECIMAL(10, 2),
    status_delivery VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_order FOREIGN KEY (id_order) 
        REFERENCES orders (id_order) ON DELETE RESTRICT
);

CREATE TRIGGER set_timestamp_deliveries
BEFORE UPDATE ON deliveries
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ========================================================================================
-- INDEXING UNTUK OPTIMASI QUERY (Enterprise Standard)
-- ========================================================================================
CREATE INDEX idx_orders_id_user ON orders(id_user);
CREATE INDEX idx_order_items_id_order ON order_items(id_order);
CREATE INDEX idx_order_items_id_prod_batch ON order_items(id_prod_batch);
CREATE INDEX idx_produk_batches_id_produk ON produk_batches(id_produk);
CREATE INDEX idx_tiering_prices_id_produk ON tiering_prices(id_produk);
CREATE INDEX idx_produk_batches_tanggal_exp ON produk_batches(tanggal_exp ASC);