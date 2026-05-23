package br.com.challenge2026.challengeFord.service;

import br.com.challenge2026.challengeFord.model.Especificacoes;
import br.com.challenge2026.challengeFord.util.InputSanitizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @PostConstruct
    void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(256 * 1024))
                .build();
    }

    public List<Especificacoes> gerarEspecificacoes(String marca,
                                                    String modelo,
                                                    String versao,
                                                    String pedidoUsuario) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Chave Gemini não configurada");
            return List.of();
        }
        String safeMarca = InputSanitizer.sanitizeForPrompt(marca, 80);
        String safeModelo = InputSanitizer.sanitizeForPrompt(modelo, 80);
        String safeVersao = InputSanitizer.sanitizeForPrompt(versao, 80);
        String safePedido = InputSanitizer.sanitizeForPrompt(pedidoUsuario, 500);

        try {
            String prompt = """
                    Você é um especialista automotivo.

                    Retorne APENAS JSON puro, sem markdown, sem explicações.

                    Formato obrigatório:

                    [
                      { "nome": "motor", "valor": "3.0 V6 Biturbo" },
                      { "nome": "torque", "valor": "583 Nm" }
                    ]

                    Veículo:
                    %s %s %s

                    Pedido do usuário (já sanitizado):
                    %s
                    """.formatted(safeMarca, safeModelo, safeVersao, safePedido);

            String requestBody = """
                    {
                      "contents": [
                        {
                          "parts": [
                            { "text": %s }
                          ]
                        }
                      ]
                    }
                    """.formatted(escapeJson(prompt));

            String resposta = webClient.post()
                    .uri(uri -> uri.path("/v1beta/models/gemini-2.5-flash:generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            String jsonLimpo = extrairTextoResposta(resposta);

            return objectMapper.readValue(
                    jsonLimpo,
                    new TypeReference<List<Especificacoes>>() {}
            );

        } catch (Exception e) {
            log.warn("Falha ao consultar serviço externo de IA (correlationId opcional via MDC)");
            return new ArrayList<>();
        }
    }

    private String extrairTextoResposta(String respostaApi) throws Exception {
        JsonNode root = objectMapper.readTree(respostaApi);
        String texto = root
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text")
                .asText();
        return texto.replace("```json", "").replace("```", "").trim();
    }

    private String escapeJson(String texto) {
        return "\"" +
                texto.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "") +
                "\"";
    }
}
