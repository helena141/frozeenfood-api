package com.lezzat.b2b2c.frozenfood.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Rampas token dari HTTP Header
            String jwt = getJwtFromRequest(request);

            // 2. Jika bawa token, cek keasliannya di Mesin Kriptografi
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                
                // 3. Baca nama/email dari token tersebut
                String username = tokenProvider.getUsernameFromJWT(jwt);

                // 4. Tarik sisa profil user dari database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. Terbitkan Paspor Resmi Spring Security
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Persilakan masuk (Buka Gerbang)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Log senyap, jangan crash-kan server hanya karena 1 penyusup
            logger.error("Gagal menetapkan autentikasi user di Security Context", ex);
        }

        // Lanjutkan request ke tujuan (Biar SecurityConfig yang menendang kalau context kosong)
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // Ekstrak ID Card dari dompet HTTP (Header "Authorization")
        String bearerToken = request.getHeader("Authorization");
        
        // Standar Mutlak Enterprise: Token harus diawali kata "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Potong 7 karakter ("Bearer ") untuk dapat token murninya
        }
        return null;
    }
}