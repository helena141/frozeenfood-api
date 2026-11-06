package com.lezzat.b2b2c.frozenfood.service.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jarak jalan ASLI lewat OSRM (Ide 5: hybrid). Default NONAKTIF.
 * Gagal / lambat / respons aneh -> Optional.empty(), dan pemanggil memakai
 * perkiraan Haversine. Sengaja tanpa library JSON (Spring Boot 4 memakai
 * Jackson 3) cukup regex untuk satu angka "distance".
 */
@Component
public class RouteDistanceClient {

    private static final Logger log = LoggerFactory.getLogger(RouteDistanceClient.class);
    private static final Pattern DISTANCE = Pattern.compile("\"routes\"\\s*:\\s*\\[\\s*\\{.*?\"distance\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.DOTALL);

    private final DeliveryProperties props;
    private final HttpClient http;

    public RouteDistanceClient(DeliveryProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getRouting().getTimeoutMs()))
                .build();
    }

    public Optional<BigDecimal> jarakKm(double asalLat, double asalLon, double tujuLat, double tujuLon) {
        if (!props.getRouting().isEnabled()) {
            return Optional.empty();
        }
        try {
            String url = String.format(Locale.ROOT, "%s/route/v1/driving/%f,%f;%f,%f?overview=false",
                    props.getRouting().getBaseUrl(), asalLon, asalLat, tujuLon, tujuLat);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(props.getRouting().getTimeoutMs()))
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("OSRM balas status {}, pakai perkiraan", res.statusCode());
                return Optional.empty();
            }
            return parseJarakKm(res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("OSRM gagal ({}), pakai perkiraan", e.toString());
            return Optional.empty();
        }
    }

    /** Dipisah supaya bisa diuji tanpa jaringan. OSRM memberi jarak dalam meter. */
    static Optional<BigDecimal> parseJarakKm(String json) {
        if (json == null) {
            return Optional.empty();
        }
        Matcher m = DISTANCE.matcher(json);
        if (!m.find()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(m.group(1))
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
    }
}
