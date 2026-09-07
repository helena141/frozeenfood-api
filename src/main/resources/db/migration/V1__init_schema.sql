
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. TABEL CUSTOMER / USER (Menggabungkan B2B, B2C, dan Admin dengan Role)
CREATE TABLE users (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- Ditambahkan untuk Spring Security
    no_telp VARCHAR(20),
    alamat TEXT,
    tipe_akun VARCHAR(20) NOT NULL, -- Enum: 'B2C', 'B2B', 'ADMIN'
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP -- Standar Soft Delete
);

-- 2. TABEL KATALOG PRODUK (Data Utama)
CREATE TABLE products (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    nama_produk VARCHAR(150) NOT NULL,
    deskripsi TEXT,
    kategori VARCHAR(50),
    harga_eceran DECIMAL(15, 2) NOT NULL,
    berat DECIMAL(10, 2) NOT NULL, -- Dibutuhkan untuk API Ongkir
    volume DECIMAL(10, 2), -- Opsional, untuk perhitungan box pengiriman
    gambar VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 3. TABEL TIERING PRICE (Sistem Cerdas B2B)
CREATE TABLE tiering_prices (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    min_qty INT NOT NULL,
    max_qty INT NOT NULL,
    harga_per_unit DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. TABEL BATCH PRODUK (Sistem Cerdas FEFO & Dynamic Pricing)
CREATE TABLE product_batches (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    stok_awal INT NOT NULL,
    stok_online INT NOT NULL,
    harga_override DECIMAL(15, 2), -- Jika ada diskon Expired
    diskon DECIMAL(5, 2) DEFAULT 0,
    tanggal_exp DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);