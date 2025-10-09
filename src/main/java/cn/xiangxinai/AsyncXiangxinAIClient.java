package cn.xiangxinai;

import cn.xiangxinai.model.*;
import cn.xiangxinai.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * XiangxinAI Guardrails asynchronous client - Context-aware AI security guardrails based on LLM
 * 
 * <p>This asynchronous client provides asynchronous interfaces for interacting with the XiangxinAI Guardrails API.
 * The guardrails use context-aware technology to understand the conversation context for security detection.
 * The asynchronous interface provides better performance and resource utilization.
 * 
 * <p>Example usage:
 * <pre>{@code
 * AsyncXiangxinAIClient client = new AsyncXiangxinAIClient("your-api-key");
 * 
 * // Async check prompt
 * CompletableFuture<GuardrailResponse> future = client.checkPromptAsync("User question");
 * GuardrailResponse result = future.get(); // Blocking wait for result
 * 
 * // Or use callback
 * client.checkPromptAsync("User question")
 *     .thenAccept(result -> {
 *         System.out.println(result.getOverallRiskLevel());
 *         System.out.println(result.getSuggestAction());
 *     })
 *     .exceptionally(throwable -> {
 *         System.err.println("Check failed: " + throwable.getMessage());
 *         return null;
 *     });
 * 
 * // Async check conversation context
 * List<Message> messages = Arrays.asList(
 *     new Message("user", "Question"),
 *     new Message("assistant", "Answer")
 * );
 * CompletableFuture<GuardrailResponse> conversationFuture = client.checkConversationAsync(messages);
 * }</pre>
 */
public class AsyncXiangxinAIClient implements AutoCloseable {
    
    private static final String DEFAULT_BASE_URL = "https://api.xiangxinai.cn/v1";
    private static final String DEFAULT_MODEL = "Xiangxin-Guardrails-Text";
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final String USER_AGENT = "xiangxinai-java-async/2.6.0";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final int maxRetries;
    
    /**
     * Constructor, using default configuration
     * 
     * @param apiKey API key
     */
    public AsyncXiangxinAIClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_TIMEOUT, DEFAULT_MAX_RETRIES);
    }
    
    /**
     * Constructor, custom configuration
     * 
     * @param apiKey API key
     * @param baseUrl API base URL
     * @param timeoutSeconds Request timeout time (seconds)
     * @param maxRetries Maximum retry times
     */
    public AsyncXiangxinAIClient(String apiKey, String baseUrl, int timeoutSeconds, int maxRetries) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/$", "") : DEFAULT_BASE_URL;
        this.maxRetries = Math.max(0, maxRetries);
        this.objectMapper = new ObjectMapper();
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + apiKey)
                            .header("Content-Type", "application/json")
                            .header("User-Agent", USER_AGENT)
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }
    
    /**
     * Create a default safe response
     */
    private GuardrailResponse createSafeResponse() {
        return new GuardrailResponse(
                "guardrails-safe-default",
                new GuardrailResult(
                        new ComplianceResult("no_risk", new ArrayList<>()),
                        new SecurityResult("no_risk", new ArrayList<>())
                ),
                "no_risk",
                "pass",
                null
        );
    }
    
    /**
     * Async check prompt security, context-aware detection
     * 
     * @param content The prompt content to check
     * @return CompletableFuture<GuardrailResponse> Async check result
     * 
     * <p>Example:
     * <pre>{@code
     * client.checkPromptAsync("I want to learn programming")
     *     .thenAccept(result -> {
     *         System.out.println(result.getOverallRiskLevel()); // "no_risk"
     *         System.out.println(result.getSuggestAction());    // "pass"
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("Check failed: " + throwable.getMessage());
     *         return null;
     *     });
     * }</pre>
     */
    public CompletableFuture<GuardrailResponse> checkPromptAsync(String content) {
        return checkPromptAsync(content, DEFAULT_MODEL);
    }
    
    /**
     * Async check prompt security, specify model
     * 
     * @param content The prompt content to check
     * @param model The model name to use
     * @return CompletableFuture<GuardrailResponse> Async check result
     */
    public CompletableFuture<GuardrailResponse> checkPromptAsync(String content, String model) {
        // If content is an empty string, return no risk
        if (content == null || content.trim().isEmpty()) {
            return CompletableFuture.completedFuture(createSafeResponse());
        }
        
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", content.trim()));
        
        GuardrailRequest request = new GuardrailRequest(model, messages);
        return makeRequestAsync("POST", "/guardrails", request);
    }
    
    /**
     * Async check conversation context security - context-aware detection
     * 
     * <p>This is the core function of the guardrails, which can understand the complete conversation context for security detection.
     * Instead of detecting each message separately, it analyzes the overall safety of the conversation.
     * 
     * <p>This is the core function of the guardrails, which can understand the complete conversation context for security detection.
     * Instead of detecting each message separately, it analyzes the overall safety of the conversation.
     * 
     * @param messages The conversation message list, containing the complete conversation of user and assistant
     * @return CompletableFuture<GuardrailResponse> Async check result
     * 
     * <p>Example:
     * <pre>{@code
     * List<Message> messages = Arrays.asList(
     *     new Message("user", "User question"),
     *     new Message("assistant", "Assistant answer")
     * );
     * client.checkConversationAsync(messages)
     *     .thenAccept(result -> {
     *         System.out.println(result.getOverallRiskLevel());
     *         if (result.isSafe()) {
     *             System.out.println("Conversation safe");
     *         } else {
     *             System.out.println("Conversation risk: " + result.getAllCategories());
     *         }
     *     });
     * }</pre>
     */
    public CompletableFuture<GuardrailResponse> checkConversationAsync(List<Message> messages) {
        return checkConversationAsync(messages, DEFAULT_MODEL);
    }
    
    /**
     * Async check conversation context security, specify model
     * 
     * @param messages The conversation message list, containing the complete conversation of user and assistant
     * @param model The model name to use
     * @return CompletableFuture<GuardrailResponse> Async check result
     */
    public CompletableFuture<GuardrailResponse> checkConversationAsync(List<Message> messages, String model) {
        if (messages == null || messages.isEmpty()) {
            CompletableFuture<GuardrailResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new ValidationException("Messages cannot be empty"));
            return future;
        }
        
        try {
            // Validate message format
            List<Message> validatedMessages = new ArrayList<>();
            boolean allEmpty = true; // Mark whether all content are empty
            
            for (Message msg : messages) {
                if (msg == null) {
                    CompletableFuture<GuardrailResponse> future = new CompletableFuture<>();
                    future.completeExceptionally(new ValidationException("Message cannot be null"));
                    return future;
                }
                
                Object content = msg.getContent();
                // Check if there is a non-empty content
                if (content != null && (!(content instanceof String) || !((String) content).trim().isEmpty())) {
                    allEmpty = false;
                    // Only add non-empty messages to validatedMessages
                    validatedMessages.add(msg);
                }
            }
            
            // If all messages' content are empty, return no risk
            if (allEmpty) {
                return CompletableFuture.completedFuture(createSafeResponse());
            }
            
            // Ensure at least one message
            if (validatedMessages.isEmpty()) {
                return CompletableFuture.completedFuture(createSafeResponse());
            }
            
            GuardrailRequest request = new GuardrailRequest(model, validatedMessages);
            return makeRequestAsync("POST", "/guardrails", request);
            
        } catch (Exception e) {
            CompletableFuture<GuardrailResponse> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Async check API service health status
     * 
     * @return CompletableFuture<Map<String, Object>> Async health status information
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> healthCheckAsync() {
        return makeRequestAsync("GET", "/guardrails/health", null)
                .thenApply(response -> {
                    // Here we need special handling, because the health check returns not GuardrailResponse
                    try {
                        String json = objectMapper.writeValueAsString(response);
                        return objectMapper.readValue(json, Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
    
    /**
     * Async get available model list
     * 
     * @return CompletableFuture<Map<String, Object>> Async model list information
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> getModelsAsync() {
        return makeRequestAsync("GET", "/guardrails/models", null)
                .thenApply(response -> {
                    // Here we need special handling, because the model list returns not GuardrailResponse
                    try {
                        String json = objectMapper.writeValueAsString(response);
                        return objectMapper.readValue(json, Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
    
    /**
     * Send asynchronous HTTP request
     */
    private CompletableFuture<GuardrailResponse> makeRequestAsync(String method, String endpoint, GuardrailRequest requestBody) {
        return makeRequestAsync(method, endpoint, requestBody, 0);
    }
    
    private CompletableFuture<GuardrailResponse> makeRequestAsync(String method, String endpoint, GuardrailRequest requestBody, int attempt) {
        String url = baseUrl + endpoint;
        
        try {
            Request.Builder requestBuilder = new Request.Builder().url(url);
            
            if ("GET".equals(method)) {
                requestBuilder.get();
            } else if ("POST".equals(method)) {
                String jsonBody = requestBody != null ? objectMapper.writeValueAsString(requestBody) : "{}";
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
                requestBuilder.post(body);
            } else {
                CompletableFuture<GuardrailResponse> future = new CompletableFuture<>();
                future.completeExceptionally(new XiangxinAIException("Unsupported HTTP method: " + method));
                return future;
            }
            
            CompletableFuture<GuardrailResponse> future = new CompletableFuture<>();
            
            httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (attempt < maxRetries) {
                        // Retry
                        CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)
                                .execute(() -> {
                                    makeRequestAsync(method, endpoint, requestBody, attempt + 1)
                                            .whenComplete((result, throwable) -> {
                                                if (throwable != null) {
                                                    future.completeExceptionally(throwable);
                                                } else {
                                                    future.complete(result);
                                                }
                                            });
                                });
                    } else {
                        future.completeExceptionally(new NetworkException("Network error: " + e.getMessage(), e));
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (Response responseToClose = response) {
                        handleAsyncResponse(responseToClose, endpoint, future, method, requestBody, attempt);
                    }
                }
            });
            
            return future;
            
        } catch (Exception e) {
            CompletableFuture<GuardrailResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new XiangxinAIException("Request setup failed: " + e.getMessage(), e));
            return future;
        }
    }
    
    /**
     * Handle asynchronous HTTP response
     */
    private void handleAsyncResponse(Response response, String endpoint, CompletableFuture<GuardrailResponse> future,
                                   String method, GuardrailRequest requestBody, int attempt) throws IOException {
        String responseBody = response.body() != null ? response.body().string() : "";
        
        if (response.isSuccessful()) {
            try {
                GuardrailResponse result = objectMapper.readValue(responseBody, GuardrailResponse.class);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(new XiangxinAIException("Failed to parse response", e));
            }
            return;
        }
        
        switch (response.code()) {
            case 401:
                future.completeExceptionally(new AuthenticationException("Invalid API key"));
                break;
            case 422:
                try {
                    JsonNode errorNode = objectMapper.readTree(responseBody);
                    String detail = errorNode.has("detail") ? errorNode.get("detail").asText() : "Validation error";
                    future.completeExceptionally(new ValidationException("Validation error: " + detail));
                } catch (Exception e) {
                    future.completeExceptionally(new ValidationException("Validation error: " + responseBody));
                }
                break;
            case 429:
                if (attempt < maxRetries) {
                    // Exponential backoff retry
                    int waitTime = (int) Math.pow(2, attempt) * 1000 + 1000;
                    CompletableFuture.delayedExecutor(waitTime, TimeUnit.MILLISECONDS)
                            .execute(() -> {
                                makeRequestAsync(method, endpoint, requestBody, attempt + 1)
                                        .whenComplete((result, throwable) -> {
                                            if (throwable != null) {
                                                future.completeExceptionally(throwable);
                                            } else {
                                                future.complete(result);
                                            }
                                        });
                            });
                } else {
                    future.completeExceptionally(new RateLimitException("Rate limit exceeded"));
                }
                break;
            default:
                String errorMsg = responseBody;
                try {
                    JsonNode errorNode = objectMapper.readTree(responseBody);
                    errorMsg = errorNode.has("detail") ? errorNode.get("detail").asText() : responseBody;
                } catch (Exception ignored) {
                    // Use original response body
                }
                
                if (attempt < maxRetries) {
                    // Retry other errors
                    CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)
                            .execute(() -> {
                                makeRequestAsync(method, endpoint, requestBody, attempt + 1)
                                        .whenComplete((result, throwable) -> {
                                            if (throwable != null) {
                                                future.completeExceptionally(throwable);
                                            } else {
                                                future.complete(result);
                                            }
                                        });
                            });
                } else {
                    future.completeExceptionally(new XiangxinAIException(
                            "API request failed with status " + response.code() + ": " + errorMsg));
                }
                break;
        }
    }
    
    /**
     * Close HTTP client resources
     */
    @Override
    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}