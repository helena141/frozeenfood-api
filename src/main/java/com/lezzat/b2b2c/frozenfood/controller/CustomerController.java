package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.CustomerUpdateRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.CustomerResponse;
import com.lezzat.b2b2c.frozenfood.service.CustomerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // =========================================================================
    // JALUR PELANGGAN MANDIRI (Aktor: B2C & B2B)
    // =========================================================================

    // [Lihat Akun Pelanggan] - B2C & B2B melihat data profil sendiri
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(customerService.getMyProfile(authentication.getName()));
    }

    // [Edit Akun Pelanggan] - B2C & B2B mengubah data profil/password sendiri
    @PutMapping("/me")
    public ResponseEntity<CustomerResponse> updateMyProfile(
            Authentication authentication,
            @RequestBody CustomerUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateMyProfile(authentication.getName(), request));
    }

    // [Upload Foto Profil] - B2C & B2B mengunggah/mengganti foto profil sendiri
    @PutMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomerResponse> uploadMyPhoto(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(customerService.uploadMyPhoto(authentication.getName(), file));
    }

    // [Hapus Akun Pelanggan] - B2C & B2B menonaktifkan akun sendiri (Soft Delete)
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMyAccount(Authentication authentication) {
        customerService.deleteMyAccount(authentication.getName());
        return ResponseEntity.ok("Akun Anda berhasil dinonaktifkan.");
    }

    // =========================================================================
    // JALUR MANAJEMEN ADMIN (Aktor: Admin)
    // =========================================================================

    // [Lihat Daftar Pelanggan] - Admin melihat seluruh daftar pelanggan & filter B2B/B2C
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(customerService.getAllCustomers(role, search));
    }

    // [Lihat Akun Pelanggan] - Admin melihat detail spesifik 1 pelanggan via ID
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    // [Hapus Akun Pelanggan] - Admin menghapus/memblokir akun pelanggan via ID (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCustomerById(@PathVariable UUID id) {
        customerService.deleteCustomerById(id);
        return ResponseEntity.ok("Akun pelanggan berhasil dihapus.");
    }
}