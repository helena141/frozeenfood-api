package com.lezzat.b2b2c.frozenfood.security;

import com.lezzat.b2b2c.frozenfood.entity.Customer;
import com.lezzat.b2b2c.frozenfood.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
class UserDetailsServiceImpl implements UserDetailsService {

    // Inject jalur akses database yang udah lu buat tadi
    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Buka brankas database, cari profil pengunjung berdasarkan email
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Penyusup ditolak: Email tidak terdaftar di database - " + email));

       // 2. Cek pangkat pengunjung (B2C, B2B, atau ADMIN) dan terjemahkan ke bahasa Keamanan
        String roleName = "ROLE_" + customer.getTipeAkun().toUpperCase(); 
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(roleName));

        // 3. Cetak dan serahkan profil standar yang bisa dibaca oleh mesin Spring Security
        return new User(
                customer.getEmail(),
                customer.getPassword(), // Password asli (sudah di-hash di DB) untuk komparasi saat login
                authorities
        );
    }
}