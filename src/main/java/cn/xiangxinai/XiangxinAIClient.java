package cn.xiangxinai;

import cn.xiangxinai.model.*;
import cn.xiangxinai.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.Base64;

/**
 * XiangxinAI Guardrails client - Context-aware AI security guardrails based on LLM
 * 
 * <p>This client provides a simple interface for interacting with the XiangxinAI Guardrails API.
 * Guardrails uses context-aware technology to understand the conversation context for security detection.
 * 
 * <p>Example usage:
 * <pre>{@code
 * XiangxinAIClient client = new XiangxinAIClient("your-api-key");
 * 
 * // Check prompt
 * GuardrailResponse result = client.checkPrompt("User question");
 *
 * // Check response context
 * GuardrailResponse result = client.checkResponseCtx("User question", "Assistant answer");
 *
 * // Check conversation context
 * List<Message> messages = Arrays.asList(
 *     new Message("user", "Question"),
 *     new Message("assistant", "Answer")
 * );
 * GuardrailResponse result = client.checkConversation(messages);
 * System.out.println(result.getOverallRiskLevel()); // "high_risk/medium_risk/low_risk/no_risk"
 * System.out.println(result.getSuggestAction()); // "pass/reject/replace"
 * }</pre>
 */
public class XiangxinAIClient implements AutoCloseable {
    
    private static final String DEFAULT_BASE_URL = "https://api.xiangxinai.cn/v1";
    private static final String DEFAULT_MODEL = "Xiangxin-Guardrails-Text";
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final String USER_AGENT = "xiangxinai-java/2.6.0";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final int maxRetries;
    
    /**
     * Constructor, using default configuration
     * 
     * @param apiKey API key
     */
    public XiangxinAIClient(String apiKey) {
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
    public XiangxinAIClient(String apiKey, String baseUrl, int timeoutSeconds, int maxRetries) {
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
     * Check prompt security, context-aware detection
     *
     * @param content User input content to be checked
     * @return Check result
     * @throws ValidationException Invalid input parameters
     * @throws AuthenticationException Authentication failed
     * @throws RateLimitException Exceeded rate limit
     * @throws XiangxinAIException Other API errors
     *
     * <p>Return result format:
     * <pre>{@code
     * {
     *   "id": "guardrails-xxx",
     *   "result": {
     *     "compliance": {
     *       "risk_level": "high_risk/medium_risk/low_risk/no_risk",
     *       "categories": ["violent crime", "sensitive political topics"]
     *     },
     *     "security": {
     *       "risk_level": "high_risk/medium_risk/low_risk/no_risk",
     *       "categories": ["prompt attack"]
     *     }
     *   },
     *   "overall_risk_level": "high_risk/medium_risk/low_risk/no_risk",
     *   "suggest_action": "pass/reject/replace",
     *   "suggest_answer": "Suggested answer content"
     * }
     * }</pre>
     *
     * <p>Example:
     * <pre>{@code
     * GuardrailResponse result = client.checkPrompt("I want to learn programming");
     * System.out.println(result.getOverallRiskLevel()); // "no_risk"
     * System.out.println(result.getSuggestAction()); // "pass"
     * System.out.println(result.getResult().getCompliance().getRiskLevel()); // "no_risk"
     * }</pre>
     */
    public GuardrailResponse checkPrompt(String content) {
        return checkPrompt(content, null);
    }

    /**
     * Check prompt security, context-aware detection
     *
     * @param content User input content to be checked
     * @param userId Optional, user ID of the tenant AI application, for user-level risk control and audit tracking
     * @return Check result
     * @throws ValidationException Invalid input parameters
     * @throws AuthenticationException Authentication failed
     * @throws RateLimitException Exceeded rate limit
     * @throws XiangxinAIException Other API errors
     *
     * <p>Example:
     * <pre>{@code
     * // Don't pass user ID
     * GuardrailResponse result = client.checkPrompt("I want to learn programming");
     *
     * // Pass user ID for tracking
     * GuardrailResponse result = client.checkPrompt("I want to learn programming", "user-123");
     * }</pre>
     */
    public GuardrailResponse checkPrompt(String content, String userId) {
        // If content is an empty string, return no risk
        if (content == null || content.trim().isEmpty()) {
            return createSafeResponse();
        }

        Map<String, String> requestData = new java.util.HashMap<>();
        requestData.put("input", content.trim());
        if (userId != null && !userId.trim().isEmpty()) {
            requestData.put("xxai_app_user_id", userId.trim());
        }

        return makeRequest("POST", "/guardrails/input", requestData, GuardrailResponse.class);
    }
    
    /**
     * Check conversation context security, context-aware detection
     *
     * <p>This is the core function of the guardrails, which can understand the complete conversation context for security detection.
     * Instead of detecting each message separately, it analyzes the overall safety of the conversation.
     *
     * @param messages Conversation message list, containing the complete conversation of user and assistant,
     * @return Check result, format is the same as checkPrompt
     *
     * <p>Example:
     * <pre>{@code
     * // Check the security of the conversation between user and assistant
     * List<Message> messages = Arrays.asList(
     *     new Message("user", "User question"),
     *     new Message("assistant", "Assistant answer")
     * );
     * GuardrailResponse result = client.checkConversation(messages);
     * System.out.println(result.getOverallRiskLevel()); // "no_risk"
     * System.out.println(result.getSuggestAction()); // Suggestion based on conversation context
     * }</pre>
     */
    public GuardrailResponse checkConversation(List<Message> messages) {
        return checkConversation(messages, DEFAULT_MODEL, null);
    }

    /**
     * Check conversation context security, context-aware detection
     *
     * @param messages Conversation message list
     * @param model Used model name
     * @return Check result
     */
    public GuardrailResponse checkConversation(List<Message> messages, String model) {
        return checkConversation(messages, model, null);
    }

    /**
     * Check conversation context security, context-aware detection
     *
     * @param messages Conversation message list
     * @param model Used model name
     * @param userId Optional, user ID of the tenant AI application, for user-level risk control and audit tracking
     * @return Check result
     *
     * <p>Example:
     * <pre>{@code
     * List<Message> messages = Arrays.asList(
     *     new Message("user", "User question"),
     *     new Message("assistant", "Assistant answer")
     * );
     * // Pass user ID for tracking
     * GuardrailResponse result = client.checkConversation(messages, "Xiangxin-Guardrails-Text", "user-123");
     * }</pre>
     */
    public GuardrailResponse checkConversation(List<Message> messages, String model, String userId) {
        if (messages == null || messages.isEmpty()) {
            throw new ValidationException("Messages cannot be empty");
        }

        // Validate message format
        List<Message> validatedMessages = new ArrayList<>();
        boolean allEmpty = true; // Mark whether all content are empty

        for (Message msg : messages) {
            if (msg == null) {
                throw new ValidationException("Message cannot be null");
            }

            Object content = msg.getContent();
            // Check if there is non-empty content
            if (content != null && (!(content instanceof String) || !((String) content).trim().isEmpty())) {
                allEmpty = false;
                // Only add non-empty messages to validatedMessages
                validatedMessages.add(msg);
            }
        }

        // If all messages' content are empty, return no risk
        if (allEmpty) {
            return createSafeResponse();
        }

        // Ensure at least one message
        if (validatedMessages.isEmpty()) {
            return createSafeResponse();
        }

        GuardrailRequest request = new GuardrailRequest(model, validatedMessages);

        // Add user ID
        if (userId != null && !userId.trim().isEmpty()) {
            if (request.getExtraBody() == null) {
                request.setExtraBody(new HashMap<>());
            }
            request.getExtraBody().put("xxai_app_user_id", userId.trim());
        }

        return makeRequest("POST", "/guardrails", request, GuardrailResponse.class);
    }

    /**
     * Check the security of user input and model output, context-aware detection
     *
     * <p>This is the core function of the guardrails, which can understand the context of user input and model output for security detection.
     * The guardrails will detect whether the model output is safe and compliant based on the context of the user question.
     *
     * @param prompt User input text content, used to help the guardrails understand the context semantics
     * @param response Model output text content, actual detection object
     * @return Check result, format is the same as checkPrompt
     * @throws ValidationException Invalid input parameters
     * @throws AuthenticationException Authentication failed
     * @throws RateLimitException Exceeded rate limit
     * @throws XiangxinAIException Other API errors
     *
     * <p>Example:
     * <pre>{@code
     * GuardrailResponse result = client.checkResponseCtx(
     *     "I want to learn programming",
     *     "I can teach you how to make simple home-cooked meals"
     * );
     * System.out.println(result.getOverallRiskLevel()); // "no_risk"
     * System.out.println(result.getSuggestAction()); // "pass"
     * }</pre>
     */
    public GuardrailResponse checkResponseCtx(String prompt, String response) {
        return checkResponseCtx(prompt, response, null);
    }

    /**
     * Check the security of user input and model output, context-aware detection
     *
     * @param prompt User input text content, used to help the guardrails understand the context semantics
     * @param response Model output text content, actual detection object
     * @param userId Optional, user ID of the tenant AI application, for user-level risk control and audit tracking
     * @return Check result, format is the same as checkPrompt
     *
     * <p>Example:
     * <pre>{@code
     * // Pass user ID for tracking
     * GuardrailResponse result = client.checkResponseCtx(
     *     "I want to learn programming",
     *     "I can teach you how to make simple home-cooked meals",
     *     "user-123"
     * );
     * }</pre>
     */
    public GuardrailResponse checkResponseCtx(String prompt, String response, String userId) {
        // If prompt or response is an empty string, return no risk
        if ((prompt == null || prompt.trim().isEmpty()) && (response == null || response.trim().isEmpty())) {
            return createSafeResponse();
        }

        Map<String, String> requestData = new java.util.HashMap<>();
        requestData.put("input", prompt != null ? prompt.trim() : "");
        requestData.put("output", response != null ? response.trim() : "");
        if (userId != null && !userId.trim().isEmpty()) {
            requestData.put("xxai_app_user_id", userId.trim());
        }

        return makeRequest("POST", "/guardrails/output", requestData, GuardrailResponse.class);
    }

    /**
     * Encode image to base64 format
     *
     * @param imagePath Image local path or HTTP(S) link
     * @return base64 encoded image content
     * @throws IOException Failed to read image
     */
    private String encodeBase64FromPath(String imagePath) throws IOException {
        byte[] imageData;

        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            // Get image from URL
            try (InputStream is = new URL(imagePath).openStream()) {
                imageData = is.readAllBytes();
            }
        } else {
            // Read image from local file
            imageData = Files.readAllBytes(Paths.get(imagePath));
        }

        return Base64.getEncoder().encodeToString(imageData);
    }

    /**
     * Check the security of text prompt and image, multimodal detection
     *
     * <p>Combine text semantics and image content for security detection.
     *
     * @param prompt Text prompt (can be empty)
     * @param image Image local path or HTTP(S) link (cannot be empty)
     * @return Check result
     * @throws ValidationException Invalid input parameters
     * @throws XiangxinAIException Other API errors
     *
     * <p>Example:
     * <pre>{@code
     * // Check local image
     * GuardrailResponse result = client.checkPromptImage("Is this image safe?", "/path/to/image.jpg");
     * // Check network image
     * GuardrailResponse result = client.checkPromptImage("", "https://example.com/image.jpg");
     * System.out.println(result.getOverallRiskLevel());
     * }</pre>
     */
    public GuardrailResponse checkPromptImage(String prompt, String image) {
        return checkPromptImage(prompt, image, "Xiangxin-Guardrails-VL", null);
    }

    /**
     * Check the security of text prompt and image, specify model
     *
     * @param prompt Text prompt (can be empty)
     * @param image Image local path or HTTP(S) link (cannot be empty)
     * @param model Used model name
     * @return Check result
     */
    public GuardrailResponse checkPromptImage(String prompt, String image, String model) {
        return checkPromptImage(prompt, image, model, null);
    }

    /**
     * Check the security of text prompt and image, specify model and user ID
     *
     * @param prompt Text prompt (can be empty)
     * @param image Image local path or HTTP(S) link (cannot be empty)
     * @param model Used model name
     * @param userId Optional, user ID of the tenant AI application, for user-level risk control and audit tracking
     * @return Check result
     *
     * <p>Example:
     * <pre>{@code
     * // Pass user ID for tracking
     * GuardrailResponse result = client.checkPromptImage(
     *     "Is this image safe?",
     *     "/path/to/image.jpg",
     *     "Xiangxin-Guardrails-VL",
     *     "user-123"
     * );
     * }</pre>
     */
    public GuardrailResponse checkPromptImage(String prompt, String image, String model, String userId) {
        if (image == null || image.trim().isEmpty()) {
            throw new ValidationException("Image path cannot be empty");
        }

        // Encode image
        String imageBase64;
        try {
            imageBase64 = encodeBase64FromPath(image);
        } catch (java.nio.file.NoSuchFileException e) {
            throw new ValidationException("Image file not found: " + image);
        } catch (IOException e) {
            throw new XiangxinAIException("Failed to encode image: " + e.getMessage(), e);
        }

        // Build message content
        List<Object> content = new ArrayList<>();
        if (prompt != null && !prompt.trim().isEmpty()) {
            Map<String, String> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", prompt.trim());
            content.add(textContent);
        }

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");
        Map<String, String> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:image/jpeg;base64," + imageBase64);
        imageContent.put("image_url", imageUrl);
        content.add(imageContent);

        // Create message
        Message message = new Message("user", content);
        List<Message> messages = new ArrayList<>();
        messages.add(message);

        GuardrailRequest request = new GuardrailRequest(model, messages);

        // Add user ID
        if (userId != null && !userId.trim().isEmpty()) {
            if (request.getExtraBody() == null) {
                request.setExtraBody(new HashMap<>());
            }
            request.getExtraBody().put("xxai_app_user_id", userId.trim());
        }

        return makeRequest("POST", "/guardrails", request, GuardrailResponse.class);
    }

    /**
     * Check the security of text prompt and multiple images, multimodal detection
     *
     * <p>Combine text semantics and multiple image content for security detection.
     *
     * @param prompt Text prompt (can be empty)
     * @param images Image local path or HTTP(S) link list (cannot be empty)
     * @return Check result
     * @throws ValidationException Invalid input parameters
     * @throws XiangxinAIException Other API errors
     *
     * <p>Example:
     * <pre>{@code
     * List<String> images = Arrays.asList("/path/to/image1.jpg", "https://example.com/image2.jpg");
     * GuardrailResponse result = client.checkPromptImages("Is this image safe?", images);
     * System.out.println(result.getOverallRiskLevel());
     * }</pre>
     */
    public GuardrailResponse checkPromptImages(String prompt, List<String> images) {
        return checkPromptImages(prompt, images, "Xiangxin-Guardrails-VL", null);
    }

    /**
     * Check the security of text prompt and multiple images, specify model
     *
     * @param prompt Text prompt (can be empty)
     * @param images Image local path or HTTP(S) link list (cannot be empty)
     * @param model Used model name
     * @return Check result
     */
    public GuardrailResponse checkPromptImages(String prompt, List<String> images, String model) {
        return checkPromptImages(prompt, images, model, null);
    }

    /**
     * Check the security of text prompt and multiple images, specify model and user ID
     *
     * @param prompt Text prompt (can be empty)
     * @param images Image local path or HTTP(S) link list (cannot be empty)
     * @param model Used model name
     * @param userId Optional, user ID of the tenant AI application, for user-level risk control and audit tracking
     * @return Check result
     *
     * <p>Example:
     * <pre>{@code
     * List<String> images = Arrays.asList("/path/to/image1.jpg", "https://example.com/image2.jpg");
     * // Pass user ID for tracking
     * GuardrailResponse result = client.checkPromptImages(
     *     "Is this image safe?",
     *     images,
     *     "Xiangxin-Guardrails-VL",
     *     "user-123"
     * );
     * }</pre>
     */
    public GuardrailResponse checkPromptImages(String prompt, List<String> images, String model, String userId) {
        if (images == null || images.isEmpty()) {
            throw new ValidationException("Images list cannot be empty");
        }

        // Build message content
        List<Object> content = new ArrayList<>();
        if (prompt != null && !prompt.trim().isEmpty()) {
            Map<String, String> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", prompt.trim());
            content.add(textContent);
        }

        // Encode all images
        for (String imagePath : images) {
            String imageBase64;
            try {
                imageBase64 = encodeBase64FromPath(imagePath);
            } catch (java.nio.file.NoSuchFileException e) {
                throw new ValidationException("Image file not found: " + imagePath);
            } catch (IOException e) {
                throw new XiangxinAIException("Failed to encode image " + imagePath + ": " + e.getMessage(), e);
            }

            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            Map<String, String> imageUrl = new HashMap<>();
            imageUrl.put("url", "data:image/jpeg;base64," + imageBase64);
            imageContent.put("image_url", imageUrl);
            content.add(imageContent);
        }

        // Create message
        Message message = new Message("user", content);
        List<Message> messages = new ArrayList<>();
        messages.add(message);

        GuardrailRequest request = new GuardrailRequest(model, messages);

        // Add user ID
        if (userId != null && !userId.trim().isEmpty()) {
            if (request.getExtraBody() == null) {
                request.setExtraBody(new HashMap<>());
            }
            request.getExtraBody().put("xxai_app_user_id", userId.trim());
        }

        return makeRequest("POST", "/guardrails", request, GuardrailResponse.class);
    }

    /**
     * Check API service health status
     * 
     * @return Health status information
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> healthCheck() {
        return makeRequest("GET", "/guardrails/health", null, Map.class);
    }
    
    /**
     * Get available model list
     * 
     * @return Model list information
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getModels() {
        return makeRequest("GET", "/guardrails/models", null, Map.class);
    }
    
    /**
     * Send HTTP request
     */
    private <T> T makeRequest(String method, String endpoint, Object requestBody, Class<T> responseType) {
        String url = baseUrl + endpoint;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Request.Builder requestBuilder = new Request.Builder().url(url);
                
                if ("GET".equals(method)) {
                    requestBuilder.get();
                } else if ("POST".equals(method)) {
                    String jsonBody = requestBody != null ? objectMapper.writeValueAsString(requestBody) : "{}";
                    RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
                    requestBuilder.post(body);
                } else {
                    throw new XiangxinAIException("Unsupported HTTP method: " + method);
                }
                
                try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                    return handleResponse(response, responseType, attempt);
                }
                
            } catch (IOException e) {
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new XiangxinAIException("Request interrupted", ie);
                    }
                    continue;
                }
                throw new NetworkException("Network error: " + e.getMessage(), e);
            } catch (AuthenticationException | ValidationException | RateLimitException e) {
                // These errors do not need to be retried
                throw e;
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new XiangxinAIException("Request interrupted", ie);
                    }
                    continue;
                }
                throw new XiangxinAIException("Unexpected error: " + e.getMessage(), e);
            }
        }
        
        throw new XiangxinAIException("Request failed after " + (maxRetries + 1) + " attempts");
    }
    
    /**
     * Handle HTTP response
     */
    private <T> T handleResponse(Response response, Class<T> responseType, int attempt) throws IOException {
        String responseBody = response.body() != null ? response.body().string() : "";
        
        if (response.isSuccessful()) {
            return objectMapper.readValue(responseBody, responseType);
        }
        
        switch (response.code()) {
            case 401:
                throw new AuthenticationException("Invalid API key");
            case 422:
                try {
                    JsonNode errorNode = objectMapper.readTree(responseBody);
                    String detail = errorNode.has("detail") ? errorNode.get("detail").asText() : "Validation error";
                    throw new ValidationException("Validation error: " + detail);
                } catch (Exception e) {
                    throw new ValidationException("Validation error: " + responseBody);
                }
            case 429:
                if (attempt < maxRetries) {
                    // Exponential backoff retry
                    int waitTime = (int) Math.pow(2, attempt) * 1000 + 1000;
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new XiangxinAIException("Request interrupted", ie);
                    }
                }
                throw new RateLimitException("Rate limit exceeded");
            default:
                String errorMsg = responseBody;
                try {
                    JsonNode errorNode = objectMapper.readTree(responseBody);
                    errorMsg = errorNode.has("detail") ? errorNode.get("detail").asText() : responseBody;
                } catch (Exception ignored) {
                    // Use original response body
                }
                throw new XiangxinAIException("API request failed with status " + response.code() + ": " + errorMsg);
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