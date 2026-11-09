package com.lezzat.b2b2c.frozenfood.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

    // Origin frontend yang diizinkan, dipisah koma. Default: Next.js lokal.
    // Ubah lewat application.yml: lezzat.cors.allowed-origins
    @Value("${lezzat.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

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

    // 3. Aturan CORS: izinkan frontend (browser) memanggil API ini
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition")); // agar nama file nota terbaca frontend
        config.setAllowCredentials(false); // token lewat header Authorization, bukan cookie

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // 4. Konstitusi Utama API (The Security Filter Chain)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // MATIKAN CSRF: API kita menggunakan token JWT murni, bukan session berbasis browser
                .csrf(AbstractHttpConfigurer::disable)
                // Aturan Rute (Routing Rules)
                .authorizeHttpRequests(auth -> auth
                // Preflight CORS dari browser harus lolos tanpa token
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Zona Publik: Siapapun boleh masuk (Login, Register)
                .requestMatchers("/api/auth/**").permitAll()
                // Zona Khusus: Hanya boleh diakses oleh Bony (Gudang/Admin)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Zona Hibrid: B2B dan B2C bisa melihat katalog produk (GET)
                .requestMatchers("/api/products/**").permitAll()
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
}
