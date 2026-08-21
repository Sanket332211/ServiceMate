package com.example.carservice.service;

import com.example.carservice.exception.AiServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GeminiService
 *
 * Encapsulates direct, secure backend communication with the Google Gemini AI REST API.
 * The API key is loaded strictly from environment configuration and never exposed to clients.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String apiUrl;

    @Autowired
    public GeminiService(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(25))
                .build();
    }

    /**
     * Internal constructor for testing / custom RestTemplate injection.
     */
    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper, String apiKey, String model, String apiUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
    }

    /**
     * Executes a generation request to Gemini expecting a valid JSON text response.
     */
    public String generateJson(String systemInstruction, String userPrompt) {
        return generateContent(systemInstruction, userPrompt, true);
    }

    /**
     * Executes a generation request to Gemini expecting freeform text response.
     */
    public String generateText(String systemInstruction, String userPrompt) {
        return generateContent(systemInstruction, userPrompt, false);
    }

    /**
     * Executes a prompt request to Gemini API and returns the generated text content.
     *
     * @param systemInstruction Optional system instruction to guide model behavior
     * @param userPrompt The prompt text containing the user request and context
     * @param jsonResponse Whether to enforce application/json response schema
     * @return Generated text from Gemini
     */
    public String generateContent(String systemInstruction, String userPrompt, boolean jsonResponse) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured. GEMINI_API_KEY environment variable is missing.");
            throw new AiServiceUnavailableException("AI Service is temporarily unavailable. Please configure the GEMINI_API_KEY environment variable.");
        }

        String trimmedKey = apiKey.trim();
        String baseUrl = (apiUrl != null && !apiUrl.trim().isEmpty())
                ? apiUrl.trim()
                : "https://generativelanguage.googleapis.com/v1beta/models";

        return executeGeminiCall(baseUrl, systemInstruction, userPrompt, jsonResponse, trimmedKey);
    }

    private String executeGeminiCall(String baseUrl, String systemInstruction, String userPrompt, boolean jsonResponse, String trimmedKey) {
        String cleanBaseUrl = baseUrl;
        if (cleanBaseUrl.contains("?")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.indexOf("?"));
        }
        if (cleanBaseUrl.endsWith("/")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
        }

        List<String> modelsToTry = new java.util.ArrayList<>();
        String primaryModel = (model != null && !model.trim().isEmpty()) ? model.trim() : "gemini-2.5-flash";
        if (primaryModel.startsWith("models/")) {
            primaryModel = primaryModel.substring("models/".length());
        } else if (primaryModel.startsWith("/")) {
            primaryModel = primaryModel.substring(1);
        }
        if (!isNonTextModel(primaryModel)) {
            modelsToTry.add(primaryModel);
        }

        // Dynamically discover supported models enabled for this API key
        List<String> discoveredModels = discoverSupportedModels(cleanBaseUrl, trimmedKey);
        for (String dm : discoveredModels) {
            if (!modelsToTry.contains(dm)) {
                modelsToTry.add(dm);
            }
        }

        // Add standard stable fallbacks in priority order
        List<String> defaultFallbacks = List.of(
                "gemini-2.5-flash",
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite",
                "gemini-3.7-flash",
                "gemini-2.5-pro",
                "gemini-1.5-flash",
                "gemini-1.5-pro"
        );
        for (String df : defaultFallbacks) {
            if (!modelsToTry.contains(df)) {
                modelsToTry.add(df);
            }
        }

        HttpStatusCodeException lastHttpError = null;

        for (String currentModel : modelsToTry) {
            String targetUrl = cleanBaseUrl + "/" + currentModel + ":generateContent";
            try {
                Map<String, Object> requestPayload = new HashMap<>();

                // 1. User Prompt
                Map<String, Object> userPart = Map.of("text", userPrompt);
                Map<String, Object> contentItem = Map.of("role", "user", "parts", List.of(userPart));
                requestPayload.put("contents", List.of(contentItem));

                // 2. System Instruction
                if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
                    Map<String, Object> sysPart = Map.of("text", systemInstruction);
                    requestPayload.put("systemInstruction", Map.of("parts", List.of(sysPart)));
                }

                // 3. Generation Config
                Map<String, Object> generationConfig = new HashMap<>();
                generationConfig.put("temperature", 0.2);
                generationConfig.put("maxOutputTokens", 1000);
                if (jsonResponse) {
                    generationConfig.put("responseMimeType", "application/json");
                }
                requestPayload.put("generationConfig", generationConfig);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("x-goog-api-key", trimmedKey);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);

                ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    log.error("Gemini API returned non-2xx status: {}", response.getStatusCode());
                    throw new AiServiceUnavailableException("AI service returned an unexpected response. Please try again later.");
                }

                try {
                    JsonNode rootNode = objectMapper.readTree(response.getBody());
                    JsonNode candidates = rootNode.path("candidates");
                    if (candidates.isArray() && candidates.size() > 0) {
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (parts.isArray() && parts.size() > 0) {
                            return parts.get(0).path("text").asText();
                        }
                    }
                } catch (Exception parseEx) {
                    log.error("Failed to parse Gemini response payload: {}", sanitizeLog(parseEx.getMessage()));
                    throw new AiServiceUnavailableException("AI service response could not be parsed.");
                }

                log.warn("Gemini response did not contain text candidates.");
                throw new AiServiceUnavailableException("AI service was unable to generate content for this request.");

            } catch (HttpClientErrorException.NotFound | HttpClientErrorException.BadRequest | HttpClientErrorException.TooManyRequests | HttpServerErrorException ex) {
                log.warn("Gemini model {} returned status {} ({}). Trying next available model...", currentModel, ex.getStatusCode(), sanitizeLog(ex.getMessage()));
                lastHttpError = ex;
            } catch (HttpClientErrorException ex) {
                String rawBody = ex.getResponseBodyAsString();
                String sanitizedError = sanitizeLog(rawBody);
                log.error("Gemini API HTTP Error status={} response={}", ex.getStatusCode(), sanitizedError);

                String detail = "Status " + ex.getStatusCode().value();
                try {
                    JsonNode errJson = objectMapper.readTree(rawBody);
                    String apiMsg = errJson.path("error").path("message").asText();
                    if (apiMsg != null && !apiMsg.trim().isEmpty()) {
                        detail += ": " + sanitizeLog(apiMsg);
                    }
                } catch (Exception ignored) {
                    // If not JSON, use sanitized status text
                }

                throw new AiServiceUnavailableException("AI service is temporarily unavailable (" + detail + "). Please verify your Gemini configuration.");
            } catch (ResourceAccessException ex) {
                log.error("Gemini API connection timeout / network failure: {}", sanitizeLog(ex.getMessage()));
                throw new AiServiceUnavailableException("AI service connection timed out. Please try again later.");
            } catch (AiServiceUnavailableException ex) {
                throw ex;
            } catch (Exception ex) {
                log.error("Unexpected error during Gemini API call: {}", sanitizeLog(ex.getMessage()), ex);
                throw new AiServiceUnavailableException("AI service is temporarily unavailable. Please try again later.");
            }
        }

        if (lastHttpError != null) {
            String rawBody = lastHttpError.getResponseBodyAsString();
            String sanitizedError = sanitizeLog(rawBody);
            log.error("All Gemini candidate models failed: {}", sanitizedError);
            String detail = "Status " + lastHttpError.getStatusCode().value();
            try {
                JsonNode errJson = objectMapper.readTree(rawBody);
                String apiMsg = errJson.path("error").path("message").asText();
                if (apiMsg != null && !apiMsg.trim().isEmpty()) {
                    detail += ": " + sanitizeLog(apiMsg);
                }
            } catch (Exception ignored) {}
            throw new AiServiceUnavailableException("AI service is temporarily unavailable (" + detail + "). Please verify your Gemini configuration.");
        }

        throw new AiServiceUnavailableException("AI service was unable to process the request.");
    }

    private List<String> discoverSupportedModels(String cleanBaseUrl, String trimmedKey) {
        List<String> discovered = new java.util.ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-goog-api-key", trimmedKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(cleanBaseUrl, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode modelsNode = root.path("models");
                if (modelsNode.isArray()) {
                    for (JsonNode m : modelsNode) {
                        String name = m.path("name").asText();
                        if (isNonTextModel(name)) {
                            continue;
                        }

                        JsonNode methods = m.path("supportedGenerationMethods");
                        boolean supportsGenerate = false;
                        if (methods.isArray()) {
                            for (JsonNode method : methods) {
                                if ("generateContent".equalsIgnoreCase(method.asText())) {
                                    supportsGenerate = true;
                                    break;
                                }
                            }
                        }
                        if (supportsGenerate && name != null && name.contains("gemini")) {
                            String cleanName = name.startsWith("models/") ? name.substring("models/".length()) : name;
                            discovered.add(cleanName);
                        }
                    }
                }
            }

            discovered.sort((a, b) -> Integer.compare(getModelPriority(b), getModelPriority(a)));

        } catch (Exception ex) {
            log.debug("Dynamic model discovery via GET {} completed: {}", cleanBaseUrl, sanitizeLog(ex.getMessage()));
        }
        return discovered;
    }

    private boolean isNonTextModel(String modelName) {
        if (modelName == null) return true;
        String lower = modelName.toLowerCase();
        return lower.contains("-tts")
                || lower.contains("text-to-speech")
                || lower.contains("-image")
                || lower.contains("-audio")
                || lower.contains("embedding")
                || lower.contains("imagen")
                || lower.contains("aqa")
                || lower.contains("robotics")
                || lower.contains("vision-preview");
    }

    private int getModelPriority(String modelName) {
        if (modelName == null) return 0;
        String lower = modelName.toLowerCase();
        if (lower.contains("gemini-2.5-flash")) return 100;
        if (lower.contains("gemini-2.0-flash") && !lower.contains("lite")) return 95;
        if (lower.contains("gemini-3.7-flash")) return 90;
        if (lower.contains("gemini-2.5-pro")) return 85;
        if (lower.contains("gemini-2.0-flash-lite")) return 80;
        if (lower.contains("gemini-1.5-flash")) return 70;
        if (lower.contains("gemini-1.5-pro")) return 65;
        return 50;
    }

    private String sanitizeLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("(?i)(key|token|api[-_]?key)=[^&\\s]+", "$1=REDACTED");
    }
}
