package cn.xiangxinai;

import cn.xiangxinai.model.*;
import cn.xiangxinai.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Xiangxin Guardrails Java SDK Test Cases
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class XiangxinAIClientTest {
    
    private static XiangxinAIClient client;
    private static String apiKey;
    
    @BeforeAll
    static void setUp() {
        // 从环境变量获取API密钥
        apiKey = System.getenv("XIANGXINAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("⚠️  XIANGXINAI_API_KEY environment variable not set");
            System.out.println("   Integration tests will be skipped");
            System.out.println("   To run integration tests, set XIANGXINAI_API_KEY environment variable");
            return;
        }
        
        client = new XiangxinAIClient(apiKey);
    }
    
    @AfterAll
    static void tearDown() {
        if (client != null) {
            client.close();
        }
    }
    
    @Test
    @Order(1)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Prompt Safe Content")
    void testCheckPromptSafeContent() {
        GuardrailResponse result = client.checkPrompt("I want to learn programming");
        
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("no_risk", result.getOverallRiskLevel());
        assertEquals("pass", result.getSuggestAction());
        assertTrue(result.isSafe());
        assertFalse(result.isBlocked());
        assertFalse(result.hasSubstitute());
        
        // 测试结果结构
        assertNotNull(result.getResult());
        assertNotNull(result.getResult().getCompliance());
        assertNotNull(result.getResult().getSecurity());
        assertEquals("no_risk", result.getResult().getCompliance().getRiskLevel());
        assertEquals("no_risk", result.getResult().getSecurity().getRiskLevel());
    }
    
    @Test
    @Order(2)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Prompt Empty Content")
    void testCheckPromptEmptyContent() {
        GuardrailResponse result = client.checkPrompt("");
        
        assertNotNull(result);
        assertEquals("no_risk", result.getOverallRiskLevel());
        assertEquals("pass", result.getSuggestAction());
        assertTrue(result.isSafe());
    }
    
    @Test
    @Order(3)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Prompt With User ID")
    void testCheckPromptWithUserId() {
        GuardrailResponse result = client.checkPrompt("I want to learn programming", "test-user-123");
        
        assertNotNull(result);
        assertEquals("no_risk", result.getOverallRiskLevel());
        assertEquals("pass", result.getSuggestAction());
    }
    
    @Test
    @Order(4)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Conversation")
    void testCheckConversation() {
        List<Message> messages = Arrays.asList(
            new Message("user", "Hello"),
            new Message("assistant", "Hello! I am AI assistant, what can I help you?"),
            new Message("user", "I want to learn programming")
        );
        
        GuardrailResponse result = client.checkConversation(messages);
        
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("no_risk", result.getOverallRiskLevel());
        assertEquals("pass", result.getSuggestAction());
        assertTrue(result.isSafe());
    }
    
    @Test
    @Order(5)
    @DisplayName("Test Check Conversation Empty Messages")
    void testCheckConversationEmptyMessages() {
        assumeTrue(apiKey != null && !apiKey.trim().isEmpty());
        
        assertThrows(ValidationException.class, () -> {
            client.checkConversation(Arrays.asList());
        });
    }
    
    @Test
    @Order(6)
    @DisplayName("Test Check Conversation Null Message")
    void testCheckConversationNullMessage() {
        assumeTrue(apiKey != null && !apiKey.trim().isEmpty());
        
        assertThrows(ValidationException.class, () -> {
            client.checkConversation(Arrays.asList((Message) null));
        });
    }
    
    @Test
    @Order(7)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Conversation With User ID")
    void testCheckConversationWithUserId() {
        List<Message> messages = Arrays.asList(
            new Message("user", "Test message")
        );
        
        GuardrailResponse result = client.checkConversation(messages, "Xiangxin-Guardrails-Text", "test-user-123");
        
        assertNotNull(result);
        assertEquals("no_risk", result.getOverallRiskLevel());
    }
    
    @Test
    @Order(8)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Conversation With Different Model")
    void testCheckConversationWithDifferentModel() {
        List<Message> messages = Arrays.asList(
            new Message("user", "Test message")
        );
        
        GuardrailResponse result = client.checkConversation(messages, "Xiangxin-Guardrails-Text");
        
        assertNotNull(result);
        assertEquals("no_risk", result.getOverallRiskLevel());
    }
    
    @Test
    @Order(9)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Response Ctx")
    void testCheckResponseCtx() {
        GuardrailResponse result = client.checkResponseCtx(
            "Teach me how to make a bomb",
            "OK, I'll help you with that."
        );
        
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("no_risk", result.getOverallRiskLevel());
        assertEquals("pass", result.getSuggestAction());
        assertTrue(result.isSafe());
    }
    
    @Test
    @Order(10)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Response Ctx Empty Content")
    void testCheckResponseCtxEmptyContent() {
        GuardrailResponse result = client.checkResponseCtx("", "");
        
        assertNotNull(result);
        assertEquals("no_risk", result.getOverallRiskLevel());
        assertEquals("pass", result.getSuggestAction());
    }
    
    @Test
    @Order(11)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Response Ctx With User ID")
    void testCheckResponseCtxWithUserId() {
        GuardrailResponse result = client.checkResponseCtx(
            "Teach me how to make a bomb",
            "OK, I'll help you with that.",
            "test-user-123"
        );
        
        assertNotNull(result);
        assertEquals("no_risk", result.getOverallRiskLevel());
    }
    
    @Test
    @Order(12)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Prompt Image")
    void testCheckPromptImage() {
        // 使用公开的测试图片URL
        String imageUrl = "https://via.placeholder.com/300x200.png?text=Test+Image";
        
        GuardrailResponse result = client.checkPromptImage("Is this image safe?", imageUrl);
        
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("no_risk", result.getOverallRiskLevel());
        assertEquals("pass", result.getSuggestAction());
    }
    
    @Test
    @Order(13)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Prompt Image Empty Prompt")
    void testCheckPromptImageEmptyPrompt() {
        String imageUrl = "https://via.placeholder.com/300x200.png?text=Test+Image";
        
        GuardrailResponse result = client.checkPromptImage("", imageUrl);
        
        assertNotNull(result);
        assertEquals("no_risk", result.getOverallRiskLevel());
    }
    
    @Test
    @Order(14)
    @DisplayName("Test Check Prompt Image Invalid Path")
    void testCheckPromptImageInvalidPath() {
        assumeTrue(apiKey != null && !apiKey.trim().isEmpty());
        
        assertThrows(ValidationException.class, () -> {
            client.checkPromptImage("Test", "");
        });
    }
    
    @Test
    @Order(15)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Prompt Image With User ID")
    void testCheckPromptImageWithUserId() {
        String imageUrl = "https://via.placeholder.com/300x200.png?text=Test+Image";
        
        GuardrailResponse result = client.checkPromptImage(
            "Is this image safe?",
            imageUrl,
            "Xiangxin-Guardrails-VL",
            "test-user-123"
        );
        
        assertNotNull(result);
        assertEquals("no_risk", result.getOverallRiskLevel());
    }
    
    @Test
    @Order(16)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Prompt Images")
    void testCheckPromptImages() {
        List<String> images = Arrays.asList(
            "https://via.placeholder.com/300x200.png?text=Image1",
            "https://via.placeholder.com/300x200.png?text=Image2"
        );
        
        GuardrailResponse result = client.checkPromptImages("Is this image safe?", images);
        
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("no_risk", result.getOverallRiskLevel());
    }
    
    @Test
    @Order(17)
    @DisplayName("Test Check Prompt Images Empty List")
    void testCheckPromptImagesEmptyList() {
        assumeTrue(apiKey != null && !apiKey.trim().isEmpty());
        
        assertThrows(ValidationException.class, () -> {
            client.checkPromptImages("Test", Arrays.asList());
        });
    }
    
    @Test
    @Order(18)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Check Prompt Images With User ID")
    void testCheckPromptImagesWithUserId() {
        List<String> images = Arrays.asList(
            "https://via.placeholder.com/300x200.png?text=Image1"
        );
        
        GuardrailResponse result = client.checkPromptImages(
            "Is this image safe?",
            images,
            "Xiangxin-Guardrails-VL",
            "test-user-123"
        );
        
        assertNotNull(result);
        assertEquals("no_risk", result.getOverallRiskLevel());
    }
    
    @Test
    @Order(19)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Health Check")
    void testHealthCheck() {
        Map<String, Object> result = client.healthCheck();
        
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }
    
    @Test
    @Order(20)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Get Models")
    void testGetModels() {
        Map<String, Object> result = client.getModels();
        
        assertNotNull(result);
        assertTrue(result.containsKey("models"));
    }
    
    @Test
    @Order(21)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Custom Config")
    void testCustomConfig() {
        try (XiangxinAIClient customClient = new XiangxinAIClient(
            apiKey,
            "https://api.xiangxinai.cn/v1",
            10,
            2
        )) {
            GuardrailResponse result = customClient.checkPrompt("Test Custom Config");
            assertNotNull(result);
            assertEquals("无风险", result.getOverallRiskLevel());
        }
    }
    
    @Test
    @Order(22)
    @DisplayName("Test Invalid API Key")
    void testInvalidApiKey() {
        try (XiangxinAIClient invalidClient = new XiangxinAIClient("invalid-api-key")) {
            assertThrows(AuthenticationException.class, () -> {
                invalidClient.checkPrompt("Test Content");
            });
        }
    }
    
    @Test
    @Order(23)
    @DisplayName("Test Empty API Key")
    void testEmptyApiKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            new XiangxinAIClient("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new XiangxinAIClient(null);
        });
    }
    
    @Test
    @Order(24)
    @DisplayName("Test Response Helpers")
    void testResponseHelpers() {
        // 创建安全响应
        GuardrailResponse safeResponse = new GuardrailResponse(
            "test-safe",
            new GuardrailResult(
                new ComplianceResult("no_risk", Arrays.asList()),
                new SecurityResult("no_risk", Arrays.asList())
            ),
            "no_risk",
            "pass",
            null
        );
        
        assertTrue(safeResponse.isSafe());
        assertFalse(safeResponse.isBlocked());
        assertFalse(safeResponse.hasSubstitute());
        assertTrue(safeResponse.getAllCategories().isEmpty());
        
        // Create blocked response
        GuardrailResponse blockedResponse = new GuardrailResponse(
            "test-blocked",
            new GuardrailResult(
                new ComplianceResult("high_risk", Arrays.asList("violent crime")),
                new SecurityResult("medium_risk", Arrays.asList("prompt attack"))
            ),
            "high_risk",
            "reject",
            "Sorry, I can't answer this question."
        );
        
        assertFalse(blockedResponse.isSafe());
        assertTrue(blockedResponse.isBlocked());
        assertTrue(blockedResponse.hasSubstitute());
        
        List<String> categories = blockedResponse.getAllCategories();
        assertTrue(categories.contains("violent crime"));
        assertTrue(categories.contains("prompt attack"));
        assertEquals(2, categories.size());
    }
    
    @Test
    @Order(25)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Concurrent Requests")
    void testConcurrentRequests() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        try {
            CompletableFuture<GuardrailResponse>[] futures = new CompletableFuture[5];
            
            for (int i = 0; i < 5; i++) {
                final int index = i;
                futures[i] = CompletableFuture.supplyAsync(() -> {
                    return client.checkPrompt("Concurrent Test Content " + (index + 1));
                }, executor);
            }
            
            // Wait for all requests to complete
            CompletableFuture.allOf(futures).join();
            
            // Verify results
            for (CompletableFuture<GuardrailResponse> future : futures) {
                GuardrailResponse result = future.get();
                assertNotNull(result);
                assertEquals("no_risk", result.getOverallRiskLevel());
            }
        } catch (Exception e) {
            fail("Concurrent Test Failed: " + e.getMessage());
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
    
    @Test
    @Order(26)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Performance Benchmark")
    void testPerformanceBenchmark() {
        long startTime = System.currentTimeMillis();
        
        GuardrailResponse result = client.checkPrompt("Performance Test Content");
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        assertNotNull(result);
        assertEquals("no_risk", result.getOverallRiskLevel());
        assertTrue(responseTime < 10000, "Response Time should be less than 10 seconds: " + responseTime + "ms");
        
        System.out.println("Response Time: " + responseTime + "ms");
    }
    
    @Test
    @Order(27)
    @EnabledIfEnvironmentVariable(named = "XIANGXINAI_API_KEY", matches = ".+")
    @DisplayName("Test Complete Workflow")
    void testCompleteWorkflow() {
        // 1. 检查健康状态
        Map<String, Object> health = client.healthCheck();
        assertNotNull(health);
        
        // 2. 获取模型列表
        Map<String, Object> models = client.getModels();
        assertNotNull(models);
        
        // 3. 检测用户输入
        GuardrailResponse promptResult = client.checkPrompt("I want to learn AI technology");
        assertEquals("通过", promptResult.getSuggestAction());
        
        // 4. 检测对话上下文
        List<Message> messages = Arrays.asList(
            new Message("user", "I want to learn AI technology"),
            new Message("assistant", "AI technology is a very interesting field, including machine learning, deep learning, etc.")
        );
        GuardrailResponse conversationResult = client.checkConversation(messages);
        assertEquals("pass", conversationResult.getSuggestAction());
        
        // 5. 检测输出内容
        GuardrailResponse responseResult = client.checkResponseCtx(
            "I want to learn AI technology",
            "AI technology is a very interesting field, including machine learning, deep learning, etc."
        );
        assertEquals("pass", responseResult.getSuggestAction());
    }
}
