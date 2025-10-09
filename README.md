# Xiangxin AI Guardrails Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/cn.xiangxinai/xiangxinai-java.svg)](https://search.maven.org/artifact/cn.xiangxinai/xiangxinai-java)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Xiangxin AI Guardrails Java Client — A Context-Aware AI Safety Guardrails System Powered by LLMs.

## Overview

Xiangxin AI Guardrails is a context-aware AI safety guardrail system based on large language models (LLMs). It understands conversational context to perform intelligent safety detection. Unlike traditional keyword-based methods, our guardrail interprets deep semantic meaning and contextual relationships within conversations.

## Key Features

- 🧠 **Context Awareness** – Based on LLM-powered conversational understanding, not simple batch detection
- 🔍 **Prompt Injection Detection** – Identifies malicious prompt injections and jailbreak attacks
- 📋 **Content Compliance Detection** – Ensures compliance with fundamental security requirements for generative AI services
- 🔐 **Sensitive Data Leakage Prevention** – Detects and prevents the leakage of personal or enterprise-sensitive information
- 🧩 **User-Level Blocking Policy** – Supports risk identification and blocking strategies at the user level
- 🖼️ **Multimodal Detection** – Supports image content safety detection
- 🛠️ **Easy Integration** – Compatible with the OpenAI API format; integrate with just one line of code
- ⚡ **OpenAI-Style API** – Familiar interface design for quick adoption
- 🚀 **Sync/Async Support** – Supports both synchronous and asynchronous modes to meet diverse application needs

## Requirements

- Java 8 or later  
- Maven 3.6+ or Gradle 6.0+

## Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>cn.xiangxinai</groupId>
    <artifactId>xiangxinai-java</artifactId>
    <version>2.6.1</version>
</dependency>
````

### Gradle

Add the dependency to your `build.gradle`:

```gradle
implementation 'cn.xiangxinai:xiangxinai-java:2.6.1'
```

## Quick Start

### Basic Usage

```java
import cn.xiangxinai.XiangxinAIClient;
import cn.xiangxinai.model.*;

// Initialize client
XiangxinAIClient client = new XiangxinAIClient("your-api-key");

// Check user input
GuardrailResponse result = client.checkPrompt("User input text");
System.out.println(result.getOverallRiskLevel()); // Safe / Low / Medium / High
System.out.println(result.getSuggestAction());     // Allow / Block / Substitute

// Check input with user ID (optional)
GuardrailResponse result2 = client.checkPrompt("User input text", "user-123");

// Context-based response checking
GuardrailResponse ctxResult = client.checkResponseCtx(
    "Teach me to cook",
    "I can teach you how to make simple home dishes"
);
System.out.println(ctxResult.getOverallRiskLevel());
System.out.println(ctxResult.getSuggestAction());

// With user ID (optional)
GuardrailResponse ctxResult2 = client.checkResponseCtx(
    "Teach me to cook",
    "I can teach you how to make simple home dishes",
    "user-123"
);
```

### Conversation Context Detection (Recommended)

```java
import java.util.Arrays;
import java.util.List;

// Detect full conversation context — core feature
List<Message> messages = Arrays.asList(
    new Message("user", "User’s question"),
    new Message("assistant", "Assistant’s response"),
    new Message("user", "Follow-up question")
);

GuardrailResponse result = client.checkConversation(messages);

// Check detection result
if (result.isSafe()) {
    System.out.println("Conversation is safe");
} else if (result.isBlocked()) {
    System.out.println("Conversation has risks — block it");
} else if (result.hasSubstitute()) {
    System.out.println("Suggested safe answer: " + result.getSuggestAnswer());
}

// With user ID (optional)
GuardrailResponse result2 = client.checkConversation(messages, "Xiangxin-Guardrails-Text", "user-123");
```

### Asynchronous API (Recommended for Better Performance)

```java
import cn.xiangxinai.AsyncXiangxinAIClient;
import java.util.concurrent.CompletableFuture;

// Async client
AsyncXiangxinAIClient asyncClient = new AsyncXiangxinAIClient("your-api-key");

// Async prompt check
CompletableFuture<GuardrailResponse> future = asyncClient.checkPromptAsync("User question");

// Option 1: Block until complete
GuardrailResponse result = future.get();
System.out.println(result.getOverallRiskLevel());

// Option 2: Use callback (recommended)
asyncClient.checkPromptAsync("User question")
    .thenAccept(result -> {
        System.out.println("Async check completed:");
        System.out.println("Risk Level: " + result.getOverallRiskLevel());
        System.out.println("Suggested Action: " + result.getSuggestAction());
    })
    .exceptionally(throwable -> {
        System.err.println("Check failed: " + throwable.getMessage());
        return null;
    });

// Async conversation detection
List<Message> messages = Arrays.asList(
    new Message("user", "User question"),
    new Message("assistant", "Assistant answer")
);
asyncClient.checkConversationAsync(messages)
    .thenAccept(result -> {
        if (result.isSafe()) {
            System.out.println("Conversation is safe");
        } else {
            System.out.println("Conversation risks: " + result.getAllCategories());
        }
    });

asyncClient.close(); // Remember to close resources
```

### Multimodal Image Detection (Added in v2.3.0)

Version 2.3.0 introduces multimodal detection — analyzing both text and image semantics for safety assessment.

```java
import cn.xiangxinai.XiangxinAIClient;
import cn.xiangxinai.model.GuardrailResponse;
import java.util.Arrays;
import java.util.List;

XiangxinAIClient client = new XiangxinAIClient("your-api-key");

// Local image
GuardrailResponse result = client.checkPromptImage(
    "Is this image safe?",
    "/path/to/image.jpg"
);
System.out.println(result.getOverallRiskLevel());
System.out.println(result.getSuggestAction());

// Image URL
result = client.checkPromptImage(
    "",  // prompt optional
    "https://example.com/image.jpg"
);

// Multiple images
List<String> images = Arrays.asList(
    "/path/to/image1.jpg",
    "https://example.com/image2.jpg",
    "/path/to/image3.png"
);
result = client.checkPromptImages("Are these images safe?", images);
System.out.println(result.getOverallRiskLevel());

// With user ID (optional)
result = client.checkPromptImage(
    "Is this image safe?",
    "/path/to/image.jpg",
    "Xiangxin-Guardrails-VL",
    "user-123"
);

result = client.checkPromptImages(
    "Are these images safe?",
    images,
    "Xiangxin-Guardrails-VL",
    "user-123"
);
```

### Using try-with-resources

```java
// Sync client
try (XiangxinAIClient client = new XiangxinAIClient("your-api-key")) {
    GuardrailResponse result = client.checkPrompt("User question");
    System.out.println(result.getOverallRiskLevel());
}

// Async client
try (AsyncXiangxinAIClient asyncClient = new AsyncXiangxinAIClient("your-api-key")) {
    CompletableFuture<GuardrailResponse> future = asyncClient.checkPromptAsync("User question");
    GuardrailResponse result = future.get();
    System.out.println(result.getOverallRiskLevel());
}
```

## API Reference

### XiangxinAIClient (Synchronous Client)

#### Constructor

```java
// Default config
XiangxinAIClient client = new XiangxinAIClient("your-api-key");

// Custom config
XiangxinAIClient client = new XiangxinAIClient(
    "your-api-key",
    "https://api.xiangxinai.cn/v1",
    30, // timeout in seconds
    3   // max retries
);
```

#### Methods

##### checkPrompt(content)

Checks the safety of a single prompt.

```java
GuardrailResponse checkPrompt(String content)
GuardrailResponse checkPrompt(String content, String model)
```

**Parameters:**

* `content` (String): Content to check
* `model` (String, optional): Model name (default: `"Xiangxin-Guardrails-Text"`)

##### checkConversation(messages)

Checks the safety of a conversation (recommended).

```java
GuardrailResponse checkConversation(List<Message> messages)
GuardrailResponse checkConversation(List<Message> messages, String model)
```

##### healthCheck()

Checks the API service health.

```java
Map<String, Object> healthCheck()
```

##### getModels()

Retrieves available models.

```java
Map<String, Object> getModels()
```

### AsyncXiangxinAIClient (Asynchronous Client — Recommended)

#### Constructor

```java
AsyncXiangxinAIClient asyncClient = new AsyncXiangxinAIClient("your-api-key");

AsyncXiangxinAIClient asyncClient = new AsyncXiangxinAIClient(
    "your-api-key",
    "https://api.xiangxinai.cn/v1",
    30,
    3
);
```

#### Async Methods

##### checkPromptAsync(content)

Asynchronously checks a single prompt.

```java
CompletableFuture<GuardrailResponse> checkPromptAsync(String content)
CompletableFuture<GuardrailResponse> checkPromptAsync(String content, String model)
```

##### checkConversationAsync(messages)

Asynchronously checks conversation safety.

```java
CompletableFuture<GuardrailResponse> checkConversationAsync(List<Message> messages)
CompletableFuture<GuardrailResponse> checkConversationAsync(List<Message> messages, String model)
```

##### healthCheckAsync()

Asynchronously checks API health.

```java
CompletableFuture<Map<String, Object>> healthCheckAsync()
```

##### getModelsAsync()

Asynchronously retrieves model list.

```java
CompletableFuture<Map<String, Object>> getModelsAsync()
```

### Data Models

#### Message

```java
public class Message {
    private String role;    // "user", "system", "assistant"
    private String content; // message content
    
    public Message(String role, String content)
    // getters/setters...
}
```

#### GuardrailResponse

```java
public class GuardrailResponse {
    private String id;
    private GuardrailResult result;
    private String overallRiskLevel;
    private String suggestAction;
    private String suggestAnswer;
    private Double score;  // 检测置信度分数 (v2.4.1新增)
    
    public boolean isSafe()
    public boolean isBlocked()
    public boolean hasSubstitute()
    public List<String> getAllCategories()
    public Double getScore()
}
```

#### GuardrailResult

```java
public class GuardrailResult {
    private ComplianceResult compliance;
    private SecurityResult security;
    private DataResult data; // Added in v2.4.0
}
```

#### ComplianceResult / SecurityResult / DataResult

```java
public class ComplianceResult {
    private String riskLevel;
    private List<String> categories;
}

public class DataResult {
    private String riskLevel;
    private List<String> categories; // Detected sensitive data types
}
```

### Example Response

```json
{
  "id": "guardrails-xxx",
  "result": {
    "compliance": {
      "risk_level": "Safe",
      "categories": []
    },
    "security": {
      "risk_level": "Safe",
      "categories": []
    },
    "data": {
      "risk_level": "Safe",
      "categories": []
    }
  },
  "overall_risk_level": "Safe",
  "suggest_action": "Allow",
  "suggest_answer": null
}
```

## Exception Handling

```java
import cn.xiangxinai.exception.*;

try {
    GuardrailResponse result = client.checkPrompt("test content");
    System.out.println(result);
} catch (AuthenticationException e) {
    System.err.println("Authentication failed — check API key: " + e.getMessage());
} catch (RateLimitException e) {
    System.err.println("Rate limit exceeded — try again later: " + e.getMessage());
} catch (ValidationException e) {
    System.err.println("Invalid input: " + e.getMessage());
} catch (NetworkException e) {
    System.err.println("Network error: " + e.getMessage());
} catch (XiangxinAIException e) {
    System.err.println("API error: " + e.getMessage());
}
```

### Exception Types

* `XiangxinAIException` – Base exception
* `AuthenticationException` – Authentication failed
* `RateLimitException` – Rate limit exceeded
* `ValidationException` – Invalid input parameters
* `NetworkException` – Network errors

## Use Cases

### 1. Content Moderation

```java
String userContent = "User-generated content...";
GuardrailResponse result = client.checkPrompt(userContent);

if (!result.isSafe()) {
    System.out.println("Unsafe content: " + result.getAllCategories());
}
```

### 2. Chatbot Protection

```java
List<Message> conversation = Arrays.asList(
    new Message("user", "User question"),
    new Message("assistant", "Assistant response")
);

GuardrailResponse result = client.checkConversation(conversation);

if ("Substitute".equals(result.getSuggestAction()) && result.getSuggestAnswer() != null) {
    return result.getSuggestAnswer();
} else if (result.isBlocked()) {
    return "Sorry, I can’t answer that.";
}
```

### 3. Batch Content Checking

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatchContentChecker {
    private final XiangxinAIClient client;
    private final ExecutorService executor;
    
    public BatchContentChecker(String apiKey) {
        this.client = new XiangxinAIClient(apiKey);
        this.executor = Executors.newFixedThreadPool(10);
    }
    
    public CompletableFuture<GuardrailResponse> checkAsync(String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return client.checkPrompt(content);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    public void close() {
        client.close();
        executor.shutdown();
    }
}
```

### 4. Spring Boot Integration

```java
@Configuration
public class XiangxinAIConfig {
    
    @Value("${xiangxinai.api-key}")
    private String apiKey;
    
    @Value("${xiangxinai.base-url:https://api.xiangxinai.cn/v1}")
    private String baseUrl;
    
    @Bean
    public XiangxinAIClient xiangxinAIClient() {
        return new XiangxinAIClient(apiKey, baseUrl, 30, 3);
    }
}

@Service
public class ContentModerationService {
    
    @Autowired
    private XiangxinAIClient xiangxinAIClient;
    
    public boolean isContentSafe(String content) {
        try {
            GuardrailResponse response = xiangxinAIClient.checkPrompt(content);
            return response.isSafe();
        } catch (Exception e) {
            log.error("Content check failed", e);
            return false;
        }
    }
}
```

## Best Practices

1. **Use Contextual Detection**: Prefer `checkConversation` over `checkPrompt` for more accurate results.
2. **Resource Management**: Always close clients using try-with-resources or manually.
3. **Exception Handling**: Implement robust retry and fallback logic.
4. **Connection Reuse**: Reuse the same `XiangxinAIClient` instance for better performance.
5. **Tuning**: Adjust timeout and retry parameters as needed.
6. **Logging**: Log all detection results for auditing and improvement.

## Thread Safety

`XiangxinAIClient` is thread-safe and can be shared across threads.

## License

Apache 2.0

## Support

* Website: [https://xiangxinai.cn](https://xiangxinai.cn)
* Docs: [https://docs.xiangxinai.cn](https://docs.xiangxinai.cn)
* Issues: [https://github.com/xiangxinai/xiangxin-guardrails/issues](https://github.com/xiangxinai/xiangxin-guardrails/issues)
* Email: [wanglei@xiangxinai.cn](mailto:wanglei@xiangxinai.cn)
