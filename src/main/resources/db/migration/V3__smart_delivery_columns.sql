-- Smart Delivery: kolom tambahan di tabel deliveries.
-- Aman dijalankan ulang (IF NOT EXISTS). Nomor V3 harus unik di seluruh tim:
-- kalau sudah ada V3 dari anggota lain, ganti jadi nomor berikutnya SEBELUM migrasi dijalankan.
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS pelaksana VARCHAR(30) NOT NULL DEFAULT 'MOTOR_TOKO';
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS ongkir NUMERIC(15,2) NOT NULL DEFAULT 0;
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS sumber_jarak VARCHAR(20);
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS estimasi_tiba TIMESTAMP;
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS nama_pengantar VARCHAR(100);
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS alasan_keputusan TEXT;
