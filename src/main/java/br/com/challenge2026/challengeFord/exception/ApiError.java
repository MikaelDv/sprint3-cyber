package br.com.challenge2026.challengeFord.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String erro,
        List<String> detalhes,
        String requestId
) {
    public static ApiError of(int status, String erro, String requestId) {
        return new ApiError(Instant.now(), status, erro, List.of(), requestId);
    }
    public static ApiError of(int status, String erro, List<String> detalhes, String requestId) {
        return new ApiError(Instant.now(), status, erro, detalhes, requestId);
    }
}
