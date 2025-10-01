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
 * 象信AI安全护栏异步客户端 - 基于LLM的上下文感知AI安全护栏
 * 
 * <p>这个异步客户端提供了与象信AI安全护栏API交互的异步接口。
 * 护栏采用上下文感知技术，能够理解对话上下文进行安全检测。
 * 异步接口提供更好的性能和资源利用率。
 * 
 * <p>示例用法:
 * <pre>{@code
 * AsyncXiangxinAIClient client = new AsyncXiangxinAIClient("your-api-key");
 * 
 * // 异步检测提示词
 * CompletableFuture<GuardrailResponse> future = client.checkPromptAsync("用户问题");
 * GuardrailResponse result = future.get(); // 阻塞等待结果
 * 
 * // 或使用回调方式
 * client.checkPromptAsync("用户问题")
 *     .thenAccept(result -> {
 *         System.out.println(result.getOverallRiskLevel());
 *         System.out.println(result.getSuggestAction());
 *     })
 *     .exceptionally(throwable -> {
 *         System.err.println("检测失败: " + throwable.getMessage());
 *         return null;
 *     });
 * 
 * // 异步检测对话上下文
 * List<Message> messages = Arrays.asList(
 *     new Message("user", "问题"),
 *     new Message("assistant", "回答")
 * );
 * CompletableFuture<GuardrailResponse> conversationFuture = client.checkConversationAsync(messages);
 * }</pre>
 */
public class AsyncXiangxinAIClient implements AutoCloseable {
    
    private static final String DEFAULT_BASE_URL = "https://api.xiangxinai.cn/v1";
    private static final String DEFAULT_MODEL = "Xiangxin-Guardrails-Text";
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final String USER_AGENT = "xiangxinai-java-async/2.3.0";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final int maxRetries;
    
    /**
     * 构造函数，使用默认配置
     * 
     * @param apiKey API密钥
     */
    public AsyncXiangxinAIClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_TIMEOUT, DEFAULT_MAX_RETRIES);
    }
    
    /**
     * 构造函数，自定义配置
     * 
     * @param apiKey API密钥
     * @param baseUrl API基础URL
     * @param timeoutSeconds 请求超时时间（秒）
     * @param maxRetries 最大重试次数
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
     * 创建无风险的默认响应
     */
    private GuardrailResponse createSafeResponse() {
        return new GuardrailResponse(
                "guardrails-safe-default",
                new GuardrailResult(
                        new ComplianceResult("无风险", new ArrayList<>()),
                        new SecurityResult("无风险", new ArrayList<>())
                ),
                "无风险",
                "通过",
                null
        );
    }
    
    /**
     * 异步检测提示词的安全性
     * 
     * @param content 要检测的提示词内容
     * @return CompletableFuture<GuardrailResponse> 异步检测结果
     * 
     * <p>示例:
     * <pre>{@code
     * client.checkPromptAsync("我想学习编程")
     *     .thenAccept(result -> {
     *         System.out.println(result.getOverallRiskLevel()); // "无风险"
     *         System.out.println(result.getSuggestAction());    // "通过"
     *     })
     *     .exceptionally(throwable -> {
     *         System.err.println("检测失败: " + throwable.getMessage());
     *         return null;
     *     });
     * }</pre>
     */
    public CompletableFuture<GuardrailResponse> checkPromptAsync(String content) {
        return checkPromptAsync(content, DEFAULT_MODEL);
    }
    
    /**
     * 异步检测提示词的安全性，指定模型
     * 
     * @param content 要检测的提示词内容
     * @param model 使用的模型名称
     * @return CompletableFuture<GuardrailResponse> 异步检测结果
     */
    public CompletableFuture<GuardrailResponse> checkPromptAsync(String content, String model) {
        // 如果content是空字符串，直接返回无风险
        if (content == null || content.trim().isEmpty()) {
            return CompletableFuture.completedFuture(createSafeResponse());
        }
        
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", content.trim()));
        
        GuardrailRequest request = new GuardrailRequest(model, messages);
        return makeRequestAsync("POST", "/guardrails", request);
    }
    
    /**
     * 异步检测对话上下文的安全性 - 上下文感知检测
     * 
     * <p>这是护栏的核心功能，能够理解完整的对话上下文进行安全检测。
     * 不是分别检测每条消息，而是分析整个对话的安全性。
     * 
     * @param messages 对话消息列表，包含用户和助手的完整对话
     * @return CompletableFuture<GuardrailResponse> 异步检测结果
     * 
     * <p>示例:
     * <pre>{@code
     * List<Message> messages = Arrays.asList(
     *     new Message("user", "用户问题"),
     *     new Message("assistant", "助手回答")
     * );
     * client.checkConversationAsync(messages)
     *     .thenAccept(result -> {
     *         System.out.println(result.getOverallRiskLevel());
     *         if (result.isSafe()) {
     *             System.out.println("对话安全");
     *         } else {
     *             System.out.println("对话存在风险: " + result.getAllCategories());
     *         }
     *     });
     * }</pre>
     */
    public CompletableFuture<GuardrailResponse> checkConversationAsync(List<Message> messages) {
        return checkConversationAsync(messages, DEFAULT_MODEL);
    }
    
    /**
     * 异步检测对话上下文的安全性，指定模型
     * 
     * @param messages 对话消息列表
     * @param model 使用的模型名称
     * @return CompletableFuture<GuardrailResponse> 异步检测结果
     */
    public CompletableFuture<GuardrailResponse> checkConversationAsync(List<Message> messages, String model) {
        if (messages == null || messages.isEmpty()) {
            CompletableFuture<GuardrailResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new ValidationException("Messages cannot be empty"));
            return future;
        }
        
        try {
            // 验证消息格式
            List<Message> validatedMessages = new ArrayList<>();
            boolean allEmpty = true; // 标记是否所有content都为空
            
            for (Message msg : messages) {
                if (msg == null) {
                    CompletableFuture<GuardrailResponse> future = new CompletableFuture<>();
                    future.completeExceptionally(new ValidationException("Message cannot be null"));
                    return future;
                }
                
                Object content = msg.getContent();
                // 检查是否有非空content
                if (content != null && (!(content instanceof String) || !((String) content).trim().isEmpty())) {
                    allEmpty = false;
                    // 只添加非空消息到validatedMessages
                    validatedMessages.add(msg);
                }
            }
            
            // 如果所有messages的content都是空的，直接返回无风险
            if (allEmpty) {
                return CompletableFuture.completedFuture(createSafeResponse());
            }
            
            // 确保至少有一条消息
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
     * 异步检查API服务健康状态
     * 
     * @return CompletableFuture<Map<String, Object>> 异步健康状态信息
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> healthCheckAsync() {
        return makeRequestAsync("GET", "/guardrails/health", null)
                .thenApply(response -> {
                    // 这里需要特殊处理，因为健康检查返回的不是GuardrailResponse
                    try {
                        String json = objectMapper.writeValueAsString(response);
                        return objectMapper.readValue(json, Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
    
    /**
     * 异步获取可用模型列表
     * 
     * @return CompletableFuture<Map<String, Object>> 异步模型列表信息
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> getModelsAsync() {
        return makeRequestAsync("GET", "/guardrails/models", null)
                .thenApply(response -> {
                    // 这里需要特殊处理，因为模型列表返回的不是GuardrailResponse
                    try {
                        String json = objectMapper.writeValueAsString(response);
                        return objectMapper.readValue(json, Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
    
    /**
     * 发送异步HTTP请求
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
                        // 重试
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
     * 处理异步HTTP响应
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
                    // 指数退避重试
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
                    // 使用原始响应体
                }
                
                if (attempt < maxRetries) {
                    // 重试其他错误
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
     * 关闭HTTP客户端资源
     */
    @Override
    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}