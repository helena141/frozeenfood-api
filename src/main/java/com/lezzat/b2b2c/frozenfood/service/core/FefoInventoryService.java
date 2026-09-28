package com.lezzat.b2b2c.frozenfood.service.core;

import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import com.lezzat.b2b2c.frozenfood.repository.ProductBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FefoInventoryService {

    private final ProductBatchRepository productBatchRepository;

    // Peringatan @Autowired dihapus karena Constructor Injection di Spring Boot modern tidak memerlukannya
    public FefoInventoryService(ProductBatchRepository productBatchRepository) {
        this.productBatchRepository = productBatchRepository;
    }

    /**
     * Algoritma utama untuk mengurangi stok berdasarkan FEFO
     */
    @Transactional
    public void deductStockFefo(UUID productId, Integer quantityToDeduct) {
        
        // 1. Ambil data batch (stok online > 0) diurutkan dari yang paling cepat kedaluwarsa
        List<ProductBatch> availableBatches = productBatchRepository
               .findByProduct_IdProdukAndStokOnlineGreaterThanOrderByTanggalExpAsc(productId, 0);

        // 2. Cek apakah total stok di gudang mencukupi permintaan
        int totalAvailableStock = availableBatches.stream()
                .mapToInt(batch -> batch.getStokOnline() != null ? batch.getStokOnline() : 0)
                .sum();

        if (totalAvailableStock < quantityToDeduct) {
            throw new RuntimeException("Gagal: Stok tidak mencukupi. Permintaan: " + quantityToDeduct + ", Tersedia: " + totalAvailableStock);
        }

        // 3. Proses potong stok dari batch terlama (FEFO)
        int remainingToDeduct = quantityToDeduct;

        for (ProductBatch batch : availableBatches) {
            if (remainingToDeduct <= 0) {
                break; // Jika kuantitas sudah terpenuhi, hentikan perulangan
            }

            int currentBatchStock = batch.getStokOnline(); // <-- Diperbaiki
            
            // Tentukan jumlah yang akan diambil dari batch ini (ambil mana yang lebih kecil)
            int deductFromThisBatch = Math.min(currentBatchStock, remainingToDeduct);
            
            // Perbarui sisa stok online di batch tersebut
            batch.setStokOnline(currentBatchStock - deductFromThisBatch); // <-- Diperbaiki
            productBatchRepository.save(batch);

            // Kurangi sisa target yang belum terpenuhi
            remainingToDeduct -= deductFromThisBatch;
        }
    }
}