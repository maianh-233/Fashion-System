package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Tạo, đọc và kiểm tra chữ ký JWT dùng cho xác thực stateless. */
@Service
public class JwtService {
    public static final String TOKEN_TYPE = "Bearer";

    public static final String ACCOUNT_TYPE_USER = "USER";
    public static final String ACCOUNT_TYPE_CUSTOMER = "CUSTOMER";

    private final SecretKey signingKey;
    private final long expirationMs;

    /**
     * Khởi tạo dịch vụ từ secret Base64 và thời hạn token trong cấu hình.
     *
     * @param secret secret Base64 lấy từ environment/config
     * @param expirationMs thời hạn JWT tính bằng mili giây
     */
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * Tạo JWT chứa userId, username và danh sách role của người dùng.
     *
     * @param user người dùng đã xác thực
     * @param roles các mã vai trò của người dùng
     * @return JWT đã ký
     */
    public String generateToken(User user, List<String> roles) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("username", user.getUsername())
                .claim("accountType", ACCOUNT_TYPE_USER)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /** Tạo token cho khách hàng bằng customerId riêng, không phát sinh userId giả. */
    public String generateCustomerToken(Customer customer) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(customer.getUsername())
                .claim("customerId", customer.getId().toString())
                .claim("username", customer.getUsername())
                .claim("accountType", ACCOUNT_TYPE_CUSTOMER)
                .claim("roles", List.of(ACCOUNT_TYPE_CUSTOMER))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Kiểm tra chữ ký, hạn dùng, userId và username của JWT.
     *
     * @param token JWT cần kiểm tra
     * @param user người dùng tương ứng trong cơ sở dữ liệu
     * @return true khi token hợp lệ và thuộc về người dùng
     */
    public boolean isTokenValid(String token, User user) {
        try {
            Claims claims = parseClaims(token);
            return user.getUsername().equals(claims.getSubject())
                    && user.getId().toString().equals(claims.get("userId", String.class))
                    && ACCOUNT_TYPE_USER.equals(claims.get("accountType", String.class))
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isCustomerTokenValid(String token, Customer customer) {
        try {
            Claims claims = parseClaims(token);
            return customer.getUsername().equals(claims.getSubject())
                    && customer.getId().toString().equals(claims.get("customerId", String.class))
                    && ACCOUNT_TYPE_CUSTOMER.equals(claims.get("accountType", String.class))
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Đọc username từ JWT đã ký.
     *
     * @param token JWT đầu vào
     * @return username trong token
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Đọc userId từ JWT đã ký.
     *
     * @param token JWT đầu vào
     * @return userId trong token
     */
    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get("userId", String.class));
    }

    public UUID extractCustomerId(String token) {
        return UUID.fromString(parseClaims(token).get("customerId", String.class));
    }

    public String extractAccountType(String token) {
        return parseClaims(token).get("accountType", String.class);
    }

    public UUID extractTokenId(String token) {
        return UUID.fromString(parseClaims(token).getId());
    }

    public LocalDateTime extractExpiration(String token) {
        return LocalDateTime.ofInstant(parseClaims(token).getExpiration().toInstant(), ZoneOffset.UTC);
    }

    /**
     * Trả về thời hạn token cấu hình để đưa vào response.
     *
     * @return thời hạn JWT tính bằng mili giây
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Xác minh chữ ký rồi lấy toàn bộ claims của JWT.
     *
     * @param token JWT cần phân tích
     * @return claims đã được xác minh
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
