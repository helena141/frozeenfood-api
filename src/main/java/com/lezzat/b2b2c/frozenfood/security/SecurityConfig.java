package com.lezzat.b2b2c.frozenfood.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Mengizinkan penggunaan @PreAuthorize di Controller
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    // Inject sang Satpam yang udah lu buat
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    // 1. Hukum Kriptografi Password
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Mesin Sentral Autentikasi
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // 3. Konstitusi Utama API (The Security Filter Chain)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // AKTIFKAN CORS: Mengizinkan request lintas domain dari frontend Next.js (http://localhost:3000)
                .cors(Customizer.withDefaults())
                // MATIKAN CSRF: API kita menggunakan token JWT murni, bukan session berbasis browser
                .csrf(AbstractHttpConfigurer::disable)
                // Aturan Rute (Routing Rules)
                .authorizeHttpRequests(auth -> auth
                // Zona Publik: Siapapun boleh masuk (Login, Register)
                .requestMatchers("/api/auth/**").permitAll()
                // Zona Khusus: Hanya boleh diakses oleh Bony (Gudang/Admin)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Zona Hibrid: B2B dan B2C bisa melihat katalog produk (GET)
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/payments/callback").permitAll()
                .requestMatchers("/error").permitAll()
                // Default Hukum Sapu Jagat: Sisa endpoint lainnya WAJIB bawa Token JWT
                .anyRequest().authenticated()
                )
                // MATIKAN SESSION: Arsitektur REST API murni tidak mengingat state (Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // TEMPATKAN SATPAM: Pasang JwtAuthenticationFilter lu tepat sebelum filter bawaan Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 4. Konfigurasi CORS untuk Headless Frontend (Next.js)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Content-Disposition", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
