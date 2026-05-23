package br.com.challenge2026.challengeFord.dto;

public record TokenResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponseDTO bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponseDTO(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
