    package com.lezzat.b2b2c.frozenfood.service;

    import java.util.UUID;

    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.authentication.BadCredentialsException;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.security.core.userdetails.UserDetailsService;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import com.lezzat.b2b2c.frozenfood.dto.request.GoogleLoginRequest;
    import com.lezzat.b2b2c.frozenfood.dto.request.LoginRequest;
    import com.lezzat.b2b2c.frozenfood.dto.request.RegisterRequest;
    import com.lezzat.b2b2c.frozenfood.dto.response.AuthResponse;
    import com.lezzat.b2b2c.frozenfood.entity.Customer;
    import com.lezzat.b2b2c.frozenfood.repository.CustomerRepository;
    import com.lezzat.b2b2c.frozenfood.security.GoogleTokenVerifier;
    import com.lezzat.b2b2c.frozenfood.security.JwtTokenProvider;

    import lombok.RequiredArgsConstructor;

    @Service
    @RequiredArgsConstructor
    public class AuthService {

        // Penanda "belum diisi" untuk akun yang lahir dari Google (kolom DB NOT NULL).
        public static final String BELUM_DIISI = "-";

        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider tokenProvider;
        private final CustomerRepository customerRepository;
        private final PasswordEncoder passwordEncoder;
        private final GoogleTokenVerifier googleTokenVerifier;
        private final UserDetailsService userDetailsService;

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
            customer.setTipeAkun(request.getTipeAkun().trim().toUpperCase());
            customer.setIsActive(true); // Akun baru langsung aktif

            // 3. Simpan ke database
            customerRepository.save(customer);

            // 4. Otentikasi otomatis agar mendapat objek Authentication untuk mencetak token
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            String token = tokenProvider.generateToken(authentication);

            return toResponse(customer, token);
        }

        public AuthResponse login(LoginRequest request) {
            // 1. Serahkan verifikasi ke mesin Spring Security (memanggil UserDetailsServiceImpl)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // 2. Jika mesin tidak melempar error (berarti lolos), ambil data user untuk response
            Customer customer = customerRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

            // 3. Cetak Token JWT
            String token = tokenProvider.generateToken(authentication);

            return toResponse(customer, token);
        }

        /**
         * Login / daftar otomatis lewat Google. Token Google diverifikasi dulu
         * (tanda tangan, kedaluwarsa, audience = client ID kita, email terverifikasi).
         * Akun baru selalu B2C; ADMIN tidak boleh masuk lewat Google.
         */
        @Transactional
        public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
            GoogleTokenVerifier.GoogleProfile profile = googleTokenVerifier.verify(request.idToken());

            Customer customer = customerRepository.findByEmail(profile.email()).orElse(null);

            if (customer == null) {
                String nama = (profile.nama() != null && !profile.nama().isBlank())
                        ? profile.nama().trim()
                        : profile.email().substring(0, profile.email().indexOf('@'));

                customer = new Customer();
                customer.setNama(nama.length() > 150 ? nama.substring(0, 150) : nama);
                customer.setEmail(profile.email());
                // Password acak yang tak pernah dibagikan -> akun ini tidak bisa dimasuki lewat form password.
                customer.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                customer.setNoTelp(BELUM_DIISI);
                customer.setAlamat(BELUM_DIISI);
                customer.setTipeAkun("B2C");
                customer.setIsActive(true);
                if (profile.foto() != null && profile.foto().length() <= 255) {
                    customer.setFotoProfil(profile.foto());
                }
                customer = customerRepository.save(customer);
            } else {
                if ("ADMIN".equalsIgnoreCase(customer.getTipeAkun())) {
                    throw new BadCredentialsException("Akun admin tidak dapat masuk lewat Google");
                }
                if (Boolean.FALSE.equals(customer.getIsActive())) {
                    throw new BadCredentialsException("Akun tidak aktif");
                }
            }

            // Bentuk Authentication yang sama seperti hasil login password, lalu cetak JWT.
            UserDetails userDetails = userDetailsService.loadUserByUsername(customer.getEmail());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            String token = tokenProvider.generateToken(authentication);

            return toResponse(customer, token);
        }

        private AuthResponse toResponse(Customer customer, String token) {
            return AuthResponse.builder()
                    .idUser(customer.getIdUser())
                    .token(token)
                    .nama(customer.getNama())
                    .email(customer.getEmail())
                    .tipeAkun(customer.getTipeAkun())
                    .build();
        }
    }