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
            String jwt = getJwtFromRequest(request);
            
            // SENSOR 1: Apakah token masuk ke backend?
            System.out.println("🚨 [SATPAM] Mencoba akses: " + request.getRequestURI());
            System.out.println("🚨 [SATPAM] Token ditangkap: " + (jwt != null ? "ADA" : "KOSONG/CACAT"));

            if (StringUtils.hasText(jwt)) {
                // SENSOR 2: Apakah kriptografinya cocok?
                boolean isValid = tokenProvider.validateToken(jwt);
                System.out.println("🚨 [SATPAM] Status Kriptografi Token Valid: " + isValid);

                if (isValid) {
                    String username = tokenProvider.getUsernameFromJWT(jwt);
                    System.out.println("🚨 [SATPAM] Email dari Token: " + username);

                    // SENSOR 3: Apakah user masih ada di database?
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    System.out.println("🚨 [SATPAM] User DB Ditemukan! Otoritas: " + userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("🚨 [SATPAM] BERHASIL! Pintu akses " + request.getRequestURI() + " dibuka!");
                }
            }
        } catch (Exception ex) {
            // SENSOR 4: Kalau meledak, tampilkan error aslinya!
            System.out.println("🚨 [SATPAM] CRASH FATAL: " + ex.getMessage());
            ex.printStackTrace();
        }

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