package com.fashionsystem.fashion_system.config;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Giới hạn request theo fixed window để giảm spam và brute-force. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, RequestWindow> windows = new ConcurrentHashMap<>();
    private final AtomicInteger cleanupCounter = new AtomicInteger();
    private final int authMaxRequests;
    private final int sensitiveMaxRequests;
    private final int apiMaxRequests;
    private final long windowMs;
    private final List<String> sensitivePathPrefixes;
    private final Clock clock;

    /**
     * Khởi tạo rate limiter từ các giới hạn trong environment/config.
     *
     * @param authMaxRequests số request auth tối đa mỗi cửa sổ
     * @param sensitiveMaxRequests số request API nhạy cảm tối đa mỗi cửa sổ
     * @param apiMaxRequests số request API thông thường tối đa mỗi cửa sổ
     * @param windowSeconds độ dài cửa sổ tính bằng giây
     * @param sensitivePaths các path prefix nhạy cảm, phân tách bằng dấu phẩy
     */
    public RateLimitFilter(
            @Value("${rate-limit.auth-max-requests}") int authMaxRequests,
            @Value("${rate-limit.sensitive-max-requests}") int sensitiveMaxRequests,
            @Value("${rate-limit.api-max-requests}") int apiMaxRequests,
            @Value("${rate-limit.window-seconds}") long windowSeconds,
            @Value("${rate-limit.sensitive-paths}") String sensitivePaths) {
        this.authMaxRequests = authMaxRequests;
        this.sensitiveMaxRequests = sensitiveMaxRequests;
        this.apiMaxRequests = apiMaxRequests;
        this.windowMs = windowSeconds * 1000;
        this.sensitivePathPrefixes = Arrays.stream(sensitivePaths.split(","))
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .toList();
        this.clock = Clock.systemUTC();
    }

    /**
     * Chọn policy theo endpoint và trả 429 khi client/user vượt giới hạn.
     *
     * @param request HTTP request hiện tại
     * @param response HTTP response hiện tại
     * @param filterChain chuỗi filter còn lại
     * @throws ServletException khi filter tiếp theo gặp lỗi servlet
     * @throws IOException khi ghi response hoặc xử lý filter gặp lỗi I/O
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = resolveLimit(path);
        String key = resolveClientKey(request) + ":" + policyName(path);
        long now = clock.millis();
        cleanupExpiredWindows(now);
        RequestWindow window = windows.compute(key, (ignored, current) ->
                current == null || now >= current.resetAt()
                        ? new RequestWindow(new AtomicInteger(1), now + windowMs)
                        : increment(current));

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, limit - window.count().get())));
        response.setHeader("X-RateLimit-Reset", String.valueOf(window.resetAt()));
        if (window.count().get() > limit) {
            writeTooManyRequests(response, window.resetAt(), now);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Bỏ qua tài nguyên ngoài /api vì chúng không thuộc API policy.
     *
     * @param request request cần phân loại
     * @return true khi không cần rate limit
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    /**
     * Tăng bộ đếm của cửa sổ hiện tại theo cách atomic.
     *
     * @param window cửa sổ rate limit hiện tại
     * @return chính cửa sổ sau khi tăng bộ đếm
     */
    private RequestWindow increment(RequestWindow window) {
        window.count().incrementAndGet();
        return window;
    }

    /**
     * Dọn các cửa sổ hết hạn định kỳ để cache không tăng vô hạn theo số client.
     *
     * @param now thời điểm hiện tại tính bằng epoch milliseconds
     */
    private void cleanupExpiredWindows(long now) {
        if (cleanupCounter.incrementAndGet() % 1000 == 0) {
            windows.entrySet().removeIf(entry -> now >= entry.getValue().resetAt());
        }
    }

    /**
     * Chọn hạn mức auth, sensitive hoặc API thông thường theo path.
     *
     * @param path đường dẫn request
     * @return số request tối đa trong một cửa sổ
     */
    private int resolveLimit(String path) {
        if (isAuthEndpoint(path)) {
            return authMaxRequests;
        }
        if (isSensitiveEndpoint(path)) {
            return sensitiveMaxRequests;
        }
        return apiMaxRequests;
    }

    /**
     * Chọn tên policy để mỗi nhóm endpoint có bộ đếm riêng.
     *
     * @param path đường dẫn request
     * @return tên policy dùng trong cache key
     */
    private String policyName(String path) {
        if (isAuthEndpoint(path)) {
            return "auth";
        }
        return isSensitiveEndpoint(path) ? "sensitive" : "api";
    }

    /**
     * Nhận diện login/register là nhóm dễ bị brute-force.
     *
     * @param path đường dẫn request
     * @return true với endpoint login hoặc register
     */
    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register/")
                || path.startsWith("/api/auth/password/");
    }

    /**
     * Nhận diện endpoint nhạy cảm theo danh sách path prefix trong cấu hình.
     *
     * @param path đường dẫn request
     * @return true khi path khớp một prefix nhạy cảm
     */
    private boolean isSensitiveEndpoint(String path) {
        return sensitivePathPrefixes.stream().anyMatch(path::startsWith);
    }

    /**
     * Dùng userId đã xác thực làm key, nếu chưa đăng nhập thì dùng IP kết nối trực tiếp.
     *
     * @param request request dùng để lấy địa chỉ client
     * @return khóa định danh client cho rate limiter
     */
    private String resolveClientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            return "user:" + principal.userId();
        }
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedCustomer principal) {
            return "customer:" + principal.customerId();
        }
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * Ghi response JSON 429 và thời gian client nên thử lại.
     *
     * @param response HTTP response cần ghi
     * @param resetAt thời điểm reset cửa sổ
     * @param now thời điểm request hiện tại
     * @throws IOException khi không thể ghi response
     */
    private void writeTooManyRequests(HttpServletResponse response, long resetAt, long now) throws IOException {
        long retryAfterSeconds = Math.max(1, (resetAt - now + 999) / 1000);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\"}");
    }

    /** Lưu bộ đếm và thời điểm reset của một fixed window. */
    private record RequestWindow(AtomicInteger count, long resetAt) {
    }
}
