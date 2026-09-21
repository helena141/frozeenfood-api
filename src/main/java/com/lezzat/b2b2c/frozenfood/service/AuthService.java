package com.lezzat.b2b2c.frozenfood.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lezzat.b2b2c.frozenfood.dto.request.LoginRequest;
import com.lezzat.b2b2c.frozenfood.dto.request.RegisterRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.AuthResponse;
import com.lezzat.b2b2c.frozenfood.entity.Customer;
import com.lezzat.b2b2c.frozenfood.repository.CustomerRepository;
import com.lezzat.b2b2c.frozenfood.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Validasi Absolut: Jangan sampai ada duplikasi email di sistem
        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email sudah terdaftar di sistem!");
        }

        // 2. Rakit entitas Customer baru
        Customer customer = new Customer();
        customer.setNama(request.getNama());
        customer.setEmail(request.getEmail());
        
        // HUKUM BESI SECURITY: Password WAJIB di-hash sebelum masuk ke database
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        
        customer.setNoTelp(request.getNoTelp());
        customer.setAlamat(request.getAlamat());
        customer.setTipeAkun(request.getTipeAkun().toUpperCase());
        customer.setIsActive(true); // Akun baru langsung aktif

      // 3. Simpan ke database
        customerRepository.save(customer);

        // 4. Otentikasi otomatis agar mendapat objek Authentication untuk mencetak token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .nama(customer.getNama())
                .email(customer.getEmail())
                .tipeAkun(customer.getTipeAkun())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Serahkan verifikasi ke mesin Spring Security (memanggil UserDetailsServiceImpl lu)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Jika mesin tidak melempar error (berarti lolos), ambil data user untuk response
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        // 3. Cetak Token JWT
        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .nama(customer.getNama())
                .email(customer.getEmail())
                .tipeAkun(customer.getTipeAkun())
                .build();
    }
}