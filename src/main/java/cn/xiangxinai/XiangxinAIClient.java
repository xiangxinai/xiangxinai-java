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
import java.util.concurrent.TimeUnit;

/**
 * 象信AI安全护栏客户端 - 基于LLM的上下文感知AI安全护栏
 * 
 * <p>这个客户端提供了与象信AI安全护栏API交互的简单接口。
 * 护栏采用上下文感知技术，能够理解对话上下文进行安全检测。
 * 
 * <p>示例用法:
 * <pre>{@code
 * XiangxinAIClient client = new XiangxinAIClient("your-api-key");
 * 
 * // 检测提示词
 * GuardrailResponse result = client.checkPrompt("用户问题");
 * 
 * // 检测对话上下文
 * List<Message> messages = Arrays.asList(
 *     new Message("user", "问题"),
 *     new Message("assistant", "回答")
 * );
 * GuardrailResponse result = client.checkConversation(messages);
 * System.out.println(result.getOverallRiskLevel()); // "高风险/中风险/低风险/无风险"
 * System.out.println(result.getSuggestAction()); // "通过/阻断/代答"
 * }</pre>
 */
public class XiangxinAIClient implements AutoCloseable {
    
    private static final String DEFAULT_BASE_URL = "https://api.xiangxinai.cn/v1";
    private static final String DEFAULT_MODEL = "Xiangxin-Guardrails-Text";
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final String USER_AGENT = "xiangxinai-java/1.1.0";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final int maxRetries;
    
    /**
     * 构造函数，使用默认配置
     * 
     * @param apiKey API密钥
     */
    public XiangxinAIClient(String apiKey) {
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
     * 检测提示词的安全性
     * 
     * @param content 要检测的提示词内容
     * @return 检测结果
     * @throws ValidationException 输入参数无效
     * @throws AuthenticationException 认证失败
     * @throws RateLimitException 超出速率限制
     * @throws XiangxinAIException 其他API错误
     * 
     * <p>返回结果格式:
     * <pre>{@code
     * {
     *   "id": "guardrails-xxx",
     *   "result": {
     *     "compliance": {
     *       "risk_level": "高风险/中风险/低风险/无风险",
     *       "categories": ["暴力犯罪", "敏感政治话题"]
     *     },
     *     "security": {
     *       "risk_level": "高风险/中风险/低风险/无风险",
     *       "categories": ["提示词攻击"]
     *     }
     *   },
     *   "overall_risk_level": "高风险/中风险/低风险/无风险",
     *   "suggest_action": "通过/阻断/代答",
     *   "suggest_answer": "建议回答内容"
     * }
     * }</pre>
     * 
     * <p>示例:
     * <pre>{@code
     * GuardrailResponse result = client.checkPrompt("我想学习编程");
     * System.out.println(result.getOverallRiskLevel()); // "无风险"
     * System.out.println(result.getSuggestAction()); // "通过"
     * System.out.println(result.getResult().getCompliance().getRiskLevel()); // "无风险"
     * }</pre>
     */
    public GuardrailResponse checkPrompt(String content) {
        return checkPrompt(content, DEFAULT_MODEL);
    }
    
    /**
     * 检测提示词的安全性
     * 
     * @param content 要检测的提示词内容
     * @param model 使用的模型名称
     * @return 检测结果
     */
    public GuardrailResponse checkPrompt(String content, String model) {
        // 如果content是空字符串，直接返回无风险
        if (content == null || content.trim().isEmpty()) {
            return createSafeResponse();
        }
        
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", content.trim()));
        
        GuardrailRequest request = new GuardrailRequest(model, messages);
        return makeRequest("POST", "/guardrails", request, GuardrailResponse.class);
    }
    
    /**
     * 检测对话上下文的安全性 - 上下文感知检测
     * 
     * <p>这是护栏的核心功能，能够理解完整的对话上下文进行安全检测。
     * 不是分别检测每条消息，而是分析整个对话的安全性。
     * 
     * @param messages 对话消息列表，包含用户和助手的完整对话
     * @return 基于对话上下文的检测结果，格式与checkPrompt相同
     * 
     * <p>示例:
     * <pre>{@code
     * // 检测用户问题和助手回答的对话安全性
     * List<Message> messages = Arrays.asList(
     *     new Message("user", "用户问题"),
     *     new Message("assistant", "助手回答")
     * );
     * GuardrailResponse result = client.checkConversation(messages);
     * System.out.println(result.getOverallRiskLevel()); // "无风险"
     * System.out.println(result.getSuggestAction()); // 基于对话上下文的建议
     * }</pre>
     */
    public GuardrailResponse checkConversation(List<Message> messages) {
        return checkConversation(messages, DEFAULT_MODEL);
    }
    
    /**
     * 检测对话上下文的安全性
     * 
     * @param messages 对话消息列表
     * @param model 使用的模型名称
     * @return 检测结果
     */
    public GuardrailResponse checkConversation(List<Message> messages, String model) {
        if (messages == null || messages.isEmpty()) {
            throw new ValidationException("Messages cannot be empty");
        }
        
        // 验证消息格式
        List<Message> validatedMessages = new ArrayList<>();
        boolean allEmpty = true; // 标记是否所有content都为空
        
        for (Message msg : messages) {
            if (msg == null) {
                throw new ValidationException("Message cannot be null");
            }
            
            String content = msg.getContent();
            // 检查是否有非空content
            if (content != null && !content.trim().isEmpty()) {
                allEmpty = false;
                // 只添加非空消息到validatedMessages
                validatedMessages.add(msg);
            }
        }
        
        // 如果所有messages的content都是空的，直接返回无风险
        if (allEmpty) {
            return createSafeResponse();
        }
        
        // 确保至少有一条消息
        if (validatedMessages.isEmpty()) {
            return createSafeResponse();
        }
        
        GuardrailRequest request = new GuardrailRequest(model, validatedMessages);
        return makeRequest("POST", "/guardrails", request, GuardrailResponse.class);
    }
    
    /**
     * 检查API服务健康状态
     * 
     * @return 健康状态信息
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> healthCheck() {
        return makeRequest("GET", "/guardrails/health", null, Map.class);
    }
    
    /**
     * 获取可用模型列表
     * 
     * @return 模型列表信息
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getModels() {
        return makeRequest("GET", "/guardrails/models", null, Map.class);
    }
    
    /**
     * 发送HTTP请求
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
                // 这些错误不需要重试
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
     * 处理HTTP响应
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
                    // 指数退避重试
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
                    // 使用原始响应体
                }
                throw new XiangxinAIException("API request failed with status " + response.code() + ": " + errorMsg);
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