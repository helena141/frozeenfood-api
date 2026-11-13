-- ========================================================================================
-- MIGRATION FILE: V4__reorder_products_columns.sql
-- Tujuan: Menata ulang urutan kolom fisik pada tabel products agar harga_eceran,
--         harga_beli, dan keuntungan bersebelahan persis sesuai spesifikasi,
--         serta menghitung keuntungan = harga_eceran - harga_beli untuk data eksisting.
-- ========================================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'products' AND column_name = 'harga_beli'
    ) THEN
        ALTER TABLE products ADD COLUMN harga_beli NUMERIC(15, 2) DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'products' AND column_name = 'keuntungan'
    ) THEN
        ALTER TABLE products ADD COLUMN keuntungan NUMERIC(15, 2) DEFAULT 0;
    END IF;

    CREATE TABLE products_new (
        id_produk VARCHAR(15) PRIMARY KEY,
        nama_produk VARCHAR(200) NOT NULL,
        deskripsi_produk TEXT,
        kategori VARCHAR(100) NOT NULL,
        harga_eceran NUMERIC(15, 2) NOT NULL CHECK (harga_eceran >= 0),
        harga_beli NUMERIC(15, 2) DEFAULT 0,
        keuntungan NUMERIC(15, 2) DEFAULT 0,
        berat DECIMAL(10, 2) NOT NULL CHECK (berat > 0),
        volume DECIMAL(10, 2),
        gambar TEXT,
        is_active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        deleted_at TIMESTAMP WITH TIME ZONE NULL
    );

    INSERT INTO products_new (
        id_produk,
        nama_produk,
        deskripsi_produk,
        kategori,
        harga_eceran,
        harga_beli,
        keuntungan,
        berat,
        volume,
        gambar,
        is_active,
        created_at,
        updated_at,
        deleted_at
    )
    SELECT
        id_produk,
        nama_produk,
        deskripsi_produk,
        kategori,
        harga_eceran,
        COALESCE(harga_beli, 0),
        harga_eceran - COALESCE(harga_beli, 0),
        berat,
        volume,
        gambar,
        is_active,
        created_at,
        updated_at,
        deleted_at
    FROM products;

    ALTER TABLE tiering_prices DROP CONSTRAINT IF EXISTS fk_tiering_produk;
    ALTER TABLE produk_batches DROP CONSTRAINT IF EXISTS fk_batch_produk;

    DROP TABLE products;
    ALTER TABLE products_new RENAME TO products;

    CREATE TRIGGER set_timestamp_products
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

    ALTER TABLE tiering_prices
        ADD CONSTRAINT fk_tiering_produk FOREIGN KEY (id_produk) REFERENCES products (id_produk) ON DELETE RESTRICT;

    ALTER TABLE produk_batches
        ADD CONSTRAINT fk_batch_produk FOREIGN KEY (id_produk) REFERENCES products (id_produk) ON DELETE RESTRICT;
END $$;
