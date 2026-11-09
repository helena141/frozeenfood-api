package com.lezzat.b2b2c.frozenfood.service.core;

import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import com.lezzat.b2b2c.frozenfood.repository.ProductBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FefoInventoryService {

    private final ProductBatchRepository productBatchRepository;

    /**
     * Algoritma utama untuk mengurangi stok berdasarkan FEFO
     */
    @Transactional
    public void deductStockFefo(String productId, Integer quantityToDeduct) { // Ubah UUID jadi String
        
        // Fail-Fast: Tolak jika kuantitas pemotongan tidak valid
        if (quantityToDeduct == null || quantityToDeduct <= 0) {
            throw new IllegalArgumentException("Kuantitas pemotongan stok harus lebih dari 0");
        }

        // 1. Ambil data batch (stok online > 0) diurutkan dari yang paling cepat kedaluwarsa
        List<ProductBatch> availableBatches = productBatchRepository
                .findByProduct_IdProdukAndStokOnlineGreaterThanOrderByTanggalExpAsc(productId, 0);

        // 2. Cek apakah total stok di gudang mencukupi permintaan
        int totalAvailableStock = availableBatches.stream()
                .mapToInt(batch -> batch.getStokOnline() != null ? batch.getStokOnline() : 0)
                .sum();

        if (totalAvailableStock < quantityToDeduct) {
            throw new IllegalArgumentException("Gagal: Stok tidak mencukupi. Permintaan: " + quantityToDeduct + ", Tersedia: " + totalAvailableStock);
        }

        // 3. Proses potong stok dari batch terlama (FEFO)
        int remainingToDeduct = quantityToDeduct;

        for (ProductBatch batch : availableBatches) {
            if (remainingToDeduct <= 0) {
                break;
            }

            int currentBatchStock = batch.getStokOnline(); 
            
            // Tentukan jumlah yang akan diambil dari batch ini
            int deductFromThisBatch = Math.min(currentBatchStock, remainingToDeduct);
            
            // Perbarui sisa stok online di batch tersebut (Otomatis disimpan oleh JPA Dirty Checking)
            batch.setStokOnline(currentBatchStock - deductFromThisBatch); 

            // Kurangi sisa target yang belum terpenuhi
            remainingToDeduct -= deductFromThisBatch;
        }
    }
}