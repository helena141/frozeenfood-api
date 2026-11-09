package com.lezzat.b2b2c.frozenfood.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lezzat.b2b2c.frozenfood.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    Optional<Customer> findByEmail(String email);

    // 1. Tambahan untuk mengecek apakah email sudah dipakai user lain saat update profil
    boolean existsByEmailAndIdUserNot(String email, UUID idUser);

    // 2. Tambahan untuk Admin memfilter daftar pelanggan (B2B/B2C)
    List<Customer> findByTipeAkunIgnoreCaseOrderByCreatedAtDesc(String tipeAkun);
}