package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.request.CustomerUpdateRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.CustomerResponse;
import com.lezzat.b2b2c.frozenfood.entity.Customer;
import com.lezzat.b2b2c.frozenfood.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String UPLOAD_DIR = "uploads/profiles/";

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers(String role, String search) {
        List<Customer> customers;

        if (role != null && !role.isBlank()) {
            customers = customerRepository.findByTipeAkunIgnoreCaseOrderByCreatedAtDesc(role);
        } else {
            customers = customerRepository.findAll();
        }

        return customers.stream()
                .filter(c -> !c.getTipeAkun().equalsIgnoreCase("ADMIN"))
                .filter(c -> {
                    if (search == null || search.isBlank()) return true;
                    String keyword = search.toLowerCase();
                    return (c.getNama() != null && c.getNama().toLowerCase().contains(keyword)) ||
                           (c.getEmail() != null && c.getEmail().toLowerCase().contains(keyword)) ||
                           (c.getNoTelp() != null && c.getNoTelp().contains(keyword));
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        return mapToResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getMyProfile(String email) {
        return mapToResponse(findEntityByEmail(email));
    }

    @Transactional
    public CustomerResponse updateMyProfile(String currentEmail, CustomerUpdateRequest request) {
        Customer customer = findEntityByEmail(currentEmail);

        if (request.getNama() != null && !request.getNama().isBlank()) {
            customer.setNama(request.getNama());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (customerRepository.existsByEmailAndIdUserNot(request.getEmail(), customer.getIdUser())) {
                throw new RuntimeException("Email sudah digunakan oleh akun lain.");
            }
            customer.setEmail(request.getEmail());
        }
        if (request.getNoTelp() != null) {
            customer.setNoTelp(request.getNoTelp());
        }
        if (request.getAlamat() != null) {
            customer.setAlamat(request.getAlamat());
        }
        if (request.getPasswordBaru() != null && !request.getPasswordBaru().isBlank()) {
            customer.setPassword(passwordEncoder.encode(request.getPasswordBaru()));
        }

        customer.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse uploadMyPhoto(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File foto tidak boleh kosong.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("File harus berupa gambar (JPG, PNG, atau WEBP).");
        }

        Customer customer = findEntityByEmail(email);

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileName = customer.getIdUser() + "_" + System.currentTimeMillis() + extension;
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            customer.setFotoProfil("/" + UPLOAD_DIR + fileName);
            customer.setUpdatedAt(LocalDateTime.now());

            return mapToResponse(customerRepository.save(customer));
        } catch (IOException e) {
            throw new RuntimeException("Gagal menyimpan foto profil: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteMyAccount(String email) {
        customerRepository.delete(findEntityByEmail(email));
    }

    @Transactional
    public void deleteCustomerById(UUID id) {
        customerRepository.delete(findEntityById(id));
    }

    private Customer findEntityById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pelanggan dengan ID " + id + " tidak ditemukan."));
    }

    private Customer findEntityByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Akun dengan email " + email + " tidak ditemukan."));
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .idUser(customer.getIdUser())
                .nama(customer.getNama())
                .email(customer.getEmail())
                .noTelp(customer.getNoTelp())
                .alamat(customer.getAlamat())
                .tipeAkun(customer.getTipeAkun())
                .fotoProfil(customer.getFotoProfil())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}