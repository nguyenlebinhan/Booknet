package grp3.java.booknet.service.tracking;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Resolves a human-friendly location string from an IP address.
 * Safe fallback for localhost / private IPs -> "Hanoi, Vietnam (Local)".
 */
@Slf4j
@Service
public class IpLocationResolver {

    private static final String LOCAL_FALLBACK = "Hanoi, Vietnam (Local)";
    private static final Duration TIMEOUT = Duration.ofSeconds(1);
    private static final Pattern IPV4_LITERAL = Pattern.compile("^(?:\\d{1,3}\\.){3}\\d{1,3}$");
    private static final Pattern IPV6_LITERAL = Pattern.compile("^[0-9a-fA-F:]+$");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    // Simple in-memory cache: ip -> location
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Returns the most accurate IP from request headers.
     */
    public String extractIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String first = xForwardedFor.split(",")[0].trim();
            if (!first.isEmpty() && !"unknown".equalsIgnoreCase(first)) {
                return first;
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp.trim())) {
            return xRealIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "127.0.0.1" : remoteAddr.trim();
    }

    /**
     * Returns location for a given IP.
     * Localhost/private IPs return LOCAL_FALLBACK immediately.
     */
    public String resolve(String ip) {
        if (ip == null || ip.isBlank()) return LOCAL_FALLBACK;
        if (isLocalOrPrivate(ip)) return LOCAL_FALLBACK;
        return cache.computeIfAbsent(ip, this::lookupLocation);
    }

    private boolean isLocalOrPrivate(String ip) {
        try {
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
                return true;
            }
            // Only call InetAddress.getByName for IP literals to avoid blocking DNS lookups.
            if (!IPV4_LITERAL.matcher(ip).matches() && !(ip.contains(":") && IPV6_LITERAL.matcher(ip).matches())) {
                return true;
            }
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isLoopbackAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isAnyLocalAddress();
        } catch (Exception e) {
            return true; // fail safe
        }
    }

    private String lookupLocation(String ip) {
        try {
            // Use free ip-api.com (no key needed, 45 req/min on free tier)
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?fields=city,country,status"))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            if (body != null && body.contains("\"status\":\"success\"")) {
                String city = extractJsonField(body, "city");
                String country = extractJsonField(body, "country");
                if (city != null && country != null) {
                    return city + ", " + country;
                }
                if (country != null) {
                    return country;
                }
            }
        } catch (Exception e) {
            log.debug("IP location lookup failed for {}: {}", ip, e.getMessage());
        }
        return LOCAL_FALLBACK;
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        String value = json.substring(start, end).trim();
        return value.isEmpty() ? null : value;
    }
}
