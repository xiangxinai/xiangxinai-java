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
 * 象信AI安全护栏客户端 - 基于LLM的上下文感知AI安全护栏
 * 
 * <p>这个客户端提供了与象信AI安全护栏API交互的简单接口。
 * 护栏采用上下文感知技术，能够理解对话上下文进行安全检测。
 * 
 * <p>示例用法:
 * <pre>{@code
 * XiangxinAIClient client = new XiangxinAIClient("your-api-key");
 * 
 * // 检测用户输入
 * GuardrailResponse result = client.checkPrompt("用户问题");
 *
 * // 检测输出内容（基于上下文）
 * GuardrailResponse result = client.checkResponseCtx("用户问题", "助手回答");
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
    private static final String USER_AGENT = "xiangxinai-java/2.3.0";
    
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
     * 检测用户输入的安全性
     *
     * @param content 要检测的用户输入内容
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
        // 如果content是空字符串，直接返回无风险
        if (content == null || content.trim().isEmpty()) {
            return createSafeResponse();
        }

        Map<String, String> requestData = new java.util.HashMap<>();
        requestData.put("input", content.trim());

        return makeRequest("POST", "/guardrails/input", requestData, GuardrailResponse.class);
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
     * 检测用户输入和模型输出的安全性 - 上下文感知检测
     *
     * <p>这是护栏的核心功能，能够理解用户输入和模型输出的上下文进行安全检测。
     * 护栏会基于用户问题的上下文来检测模型输出是否安全合规。
     *
     * @param prompt 用户输入的文本内容，用于让护栏理解上下文语意
     * @param response 模型输出的文本内容，实际检测对象
     * @return 基于上下文的检测结果，格式与checkPrompt相同
     * @throws ValidationException 输入参数无效
     * @throws AuthenticationException 认证失败
     * @throws RateLimitException 超出速率限制
     * @throws XiangxinAIException 其他API错误
     *
     * <p>示例:
     * <pre>{@code
     * GuardrailResponse result = client.checkResponseCtx(
     *     "教我做饭",
     *     "我可以教你做一些简单的家常菜"
     * );
     * System.out.println(result.getOverallRiskLevel()); // "无风险"
     * System.out.println(result.getSuggestAction()); // "通过"
     * }</pre>
     */
    public GuardrailResponse checkResponseCtx(String prompt, String response) {
        // 如果prompt或response是空字符串，直接返回无风险
        if ((prompt == null || prompt.trim().isEmpty()) && (response == null || response.trim().isEmpty())) {
            return createSafeResponse();
        }

        Map<String, String> requestData = new java.util.HashMap<>();
        requestData.put("input", prompt != null ? prompt.trim() : "");
        requestData.put("output", response != null ? response.trim() : "");

        return makeRequest("POST", "/guardrails/output", requestData, GuardrailResponse.class);
    }

    /**
     * 将图片编码为base64格式
     *
     * @param imagePath 图片的本地路径或HTTP(S)链接
     * @return base64编码的图片内容
     * @throws IOException 读取图片失败
     */
    private String encodeBase64FromPath(String imagePath) throws IOException {
        byte[] imageData;

        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            // 从URL获取图片
            try (InputStream is = new URL(imagePath).openStream()) {
                imageData = is.readAllBytes();
            }
        } else {
            // 从本地文件读取
            imageData = Files.readAllBytes(Paths.get(imagePath));
        }

        return Base64.getEncoder().encodeToString(imageData);
    }

    /**
     * 检测文本提示词和图片的安全性 - 多模态检测
     *
     * <p>结合文本语义和图片内容进行安全检测。
     *
     * @param prompt 文本提示词（可以为空）
     * @param image 图片文件的本地路径或HTTP(S)链接（不能为空）
     * @return 检测结果
     * @throws ValidationException 输入参数无效
     * @throws XiangxinAIException 其他API错误
     *
     * <p>示例:
     * <pre>{@code
     * // 检测本地图片
     * GuardrailResponse result = client.checkPromptImage("这个图片安全吗？", "/path/to/image.jpg");
     * // 检测网络图片
     * GuardrailResponse result = client.checkPromptImage("", "https://example.com/image.jpg");
     * System.out.println(result.getOverallRiskLevel());
     * }</pre>
     */
    public GuardrailResponse checkPromptImage(String prompt, String image) {
        return checkPromptImage(prompt, image, "Xiangxin-Guardrails-VL");
    }

    /**
     * 检测文本提示词和图片的安全性，指定模型
     *
     * @param prompt 文本提示词（可以为空）
     * @param image 图片文件的本地路径或HTTP(S)链接（不能为空）
     * @param model 使用的模型名称
     * @return 检测结果
     */
    public GuardrailResponse checkPromptImage(String prompt, String image, String model) {
        if (image == null || image.trim().isEmpty()) {
            throw new ValidationException("Image path cannot be empty");
        }

        // 编码图片
        String imageBase64;
        try {
            imageBase64 = encodeBase64FromPath(image);
        } catch (java.nio.file.NoSuchFileException e) {
            throw new ValidationException("Image file not found: " + image);
        } catch (IOException e) {
            throw new XiangxinAIException("Failed to encode image: " + e.getMessage(), e);
        }

        // 构建消息内容
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

        // 创建消息
        Message message = new Message("user", content);
        List<Message> messages = new ArrayList<>();
        messages.add(message);

        GuardrailRequest request = new GuardrailRequest(model, messages);
        return makeRequest("POST", "/guardrails", request, GuardrailResponse.class);
    }

    /**
     * 检测文本提示词和多张图片的安全性 - 多模态检测
     *
     * <p>结合文本语义和多张图片内容进行安全检测。
     *
     * @param prompt 文本提示词（可以为空）
     * @param images 图片文件的本地路径或HTTP(S)链接列表（不能为空）
     * @return 检测结果
     * @throws ValidationException 输入参数无效
     * @throws XiangxinAIException 其他API错误
     *
     * <p>示例:
     * <pre>{@code
     * List<String> images = Arrays.asList("/path/to/image1.jpg", "https://example.com/image2.jpg");
     * GuardrailResponse result = client.checkPromptImages("这些图片安全吗？", images);
     * System.out.println(result.getOverallRiskLevel());
     * }</pre>
     */
    public GuardrailResponse checkPromptImages(String prompt, List<String> images) {
        return checkPromptImages(prompt, images, "Xiangxin-Guardrails-VL");
    }

    /**
     * 检测文本提示词和多张图片的安全性，指定模型
     *
     * @param prompt 文本提示词（可以为空）
     * @param images 图片文件的本地路径或HTTP(S)链接列表（不能为空）
     * @param model 使用的模型名称
     * @return 检测结果
     */
    public GuardrailResponse checkPromptImages(String prompt, List<String> images, String model) {
        if (images == null || images.isEmpty()) {
            throw new ValidationException("Images list cannot be empty");
        }

        // 构建消息内容
        List<Object> content = new ArrayList<>();
        if (prompt != null && !prompt.trim().isEmpty()) {
            Map<String, String> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", prompt.trim());
            content.add(textContent);
        }

        // 编码所有图片
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

        // 创建消息
        Message message = new Message("user", content);
        List<Message> messages = new ArrayList<>();
        messages.add(message);

        GuardrailRequest request = new GuardrailRequest(model, messages);
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