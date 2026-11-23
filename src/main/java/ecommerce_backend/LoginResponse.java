package ecommerce_backend;

public record LoginResponse(String token, String role, long expiresInSeconds) {
}
