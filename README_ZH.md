# 象信AI安全护栏 Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/cn.xiangxinai/xiangxinai-java.svg)](https://search.maven.org/artifact/cn.xiangxinai/xiangxinai-java)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

象信AI安全护栏 Java 客户端 - 基于LLM的上下文感知AI安全护栏。

## 概述

象信AI安全护栏是一个基于大语言模型的上下文感知AI安全护栏系统，能够理解对话上下文进行智能安全检测。不同于传统的关键词匹配，我们的护栏能够理解语言的深层含义和对话的上下文关系。

## 核心特性

- 🧠 **上下文感知** - 基于LLM的对话理解，而不是简单的批量检测
- 🔍 **提示词攻击检测** - 识别恶意提示词注入和越狱攻击
- 📋 **内容合规检测** - 满足生成式人工智能服务安全基本要求
- 🔐 **敏感数据防泄漏** - 检测和防止个人/企业敏感数据泄露
- 🧩 **用户级封禁策略** - 支持基于用户颗粒度的风险识别与封禁策略
- 🖼️ **多模态检测** - 支持图片内容安全检测
- 🛠️ **易于集成** - 兼容OpenAI API格式，一行代码接入
- ⚡ **OpenAI风格API** - 熟悉的接口设计，快速上手
- 🚀 **同步/异步支持** - 支持同步和异步两种调用方式，满足不同场景需求

## 环境要求

- Java 8 或更高版本
- Maven 3.6+ 或 Gradle 6.0+

## 安装

### Maven

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>cn.xiangxinai</groupId>
    <artifactId>xiangxinai-java</artifactId>
    <version>2.6.1</version>
</dependency>
```

### Gradle

在 `build.gradle` 中添加依赖：

```gradle
implementation 'cn.xiangxinai:xiangxinai-java:2.6.1'
```

## 快速开始

### 基本用法

```java
import cn.xiangxinai.XiangxinAIClient;
import cn.xiangxinai.model.*;

// 初始化客户端
XiangxinAIClient client = new XiangxinAIClient("your-api-key");

// 检测用户输入
GuardrailResponse result = client.checkPrompt("用户输入的问题");
System.out.println(result.getOverallRiskLevel()); // 无风险/低风险/中风险/高风险
System.out.println(result.getSuggestAction());     // 通过/阻断/代答

// 检测用户输入并传递用户ID（可选）
GuardrailResponse result2 = client.checkPrompt("用户输入的问题", "user-123");

// 检测输出内容（基于上下文）
GuardrailResponse ctxResult = client.checkResponseCtx(
    "教我做饭",
    "我可以教你做一些简单的家常菜"
);
System.out.println(ctxResult.getOverallRiskLevel()); // 无风险
System.out.println(ctxResult.getSuggestAction());     // 通过

// 检测输出内容并传递用户ID（可选）
GuardrailResponse ctxResult2 = client.checkResponseCtx(
    "教我做饭",
    "我可以教你做一些简单的家常菜",
    "user-123"
);
```

### 对话上下文检测（推荐）

```java
import java.util.Arrays;
import java.util.List;

// 检测完整对话上下文 - 核心功能
List<Message> messages = Arrays.asList(
    new Message("user", "用户的问题"),
    new Message("assistant", "AI助手的回答"),
    new Message("user", "用户的后续问题")
);

GuardrailResponse result = client.checkConversation(messages);

// 检查检测结果
if (result.isSafe()) {
    System.out.println("对话安全，可以继续");
} else if (result.isBlocked()) {
    System.out.println("对话存在风险，建议阻断");
} else if (result.hasSubstitute()) {
    System.out.println("建议使用安全回答: " + result.getSuggestAnswer());
}

// 传递用户ID用于追踪（可选）
GuardrailResponse result2 = client.checkConversation(messages, "Xiangxin-Guardrails-Text", "user-123");
```

### 异步接口（推荐，性能更好）

```java
import cn.xiangxinai.AsyncXiangxinAIClient;
import java.util.concurrent.CompletableFuture;

// 使用异步客户端
AsyncXiangxinAIClient asyncClient = new AsyncXiangxinAIClient("your-api-key");

// 异步检测提示词
CompletableFuture<GuardrailResponse> future = asyncClient.checkPromptAsync("用户问题");

// 方式1: 阻塞等待结果
GuardrailResponse result = future.get();
System.out.println(result.getOverallRiskLevel());

// 方式2: 使用回调（推荐）
asyncClient.checkPromptAsync("用户问题")
    .thenAccept(result -> {
        System.out.println("异步检测完成:");
        System.out.println("风险等级: " + result.getOverallRiskLevel());
        System.out.println("建议动作: " + result.getSuggestAction());
    })
    .exceptionally(throwable -> {
        System.err.println("检测失败: " + throwable.getMessage());
        return null;
    });

// 异步对话检测
List<Message> messages = Arrays.asList(
    new Message("user", "用户问题"),
    new Message("assistant", "助手回答")
);
asyncClient.checkConversationAsync(messages)
    .thenAccept(result -> {
        if (result.isSafe()) {
            System.out.println("对话安全");
        } else {
            System.out.println("对话存在风险: " + result.getAllCategories());
        }
    });

asyncClient.close(); // 记住关闭资源
```

### 多模态图片检测（2.3.0新增）

象信AI安全护栏2.3.0版本新增了多模态检测功能，支持图片内容安全检测，可以结合提示词文本的语义和图片内容语义分析得出是否安全。

```java
import cn.xiangxinai.XiangxinAIClient;
import cn.xiangxinai.model.GuardrailResponse;
import java.util.Arrays;
import java.util.List;

XiangxinAIClient client = new XiangxinAIClient("your-api-key");

// 检测单张图片（本地文件）
GuardrailResponse result = client.checkPromptImage(
    "这个图片安全吗？",
    "/path/to/image.jpg"
);
System.out.println(result.getOverallRiskLevel());
System.out.println(result.getSuggestAction());

// 检测单张图片（网络URL）
result = client.checkPromptImage(
    "",  // prompt可以为空
    "https://example.com/image.jpg"
);

// 检测多张图片
List<String> images = Arrays.asList(
    "/path/to/image1.jpg",
    "https://example.com/image2.jpg",
    "/path/to/image3.png"
);
result = client.checkPromptImages("这些图片都安全吗？", images);
System.out.println(result.getOverallRiskLevel());

// 传递用户ID用于追踪（可选）
result = client.checkPromptImage(
    "这个图片安全吗？",
    "/path/to/image.jpg",
    "Xiangxin-Guardrails-VL",
    "user-123"
);

result = client.checkPromptImages(
    "这些图片都安全吗？",
    images,
    "Xiangxin-Guardrails-VL",
    "user-123"
);
```

### 使用 try-with-resources

```java
// 同步客户端
try (XiangxinAIClient client = new XiangxinAIClient("your-api-key")) {
    GuardrailResponse result = client.checkPrompt("用户问题");
    System.out.println(result.getOverallRiskLevel());
}

// 异步客户端
try (AsyncXiangxinAIClient asyncClient = new AsyncXiangxinAIClient("your-api-key")) {
    CompletableFuture<GuardrailResponse> future = asyncClient.checkPromptAsync("用户问题");
    GuardrailResponse result = future.get();
    System.out.println(result.getOverallRiskLevel());
}
```

## API 参考

### XiangxinAIClient（同步客户端）

#### 构造函数

```java
// 使用默认配置
XiangxinAIClient client = new XiangxinAIClient("your-api-key");

// 自定义配置
XiangxinAIClient client = new XiangxinAIClient(
    "your-api-key",     // API密钥
    "https://api.xiangxinai.cn/v1", // API基础URL
    30,                 // 请求超时时间（秒）
    3                   // 最大重试次数
);
```

#### 方法

##### checkPrompt(content)

检测单个提示词的安全性。

```java
GuardrailResponse checkPrompt(String content)
GuardrailResponse checkPrompt(String content, String model)
```

**参数:**
- `content` (String): 要检测的内容
- `model` (String, 可选): 模型名称，默认 "Xiangxin-Guardrails-Text"

##### checkConversation(messages)

检测对话上下文的安全性（推荐使用）。

```java
GuardrailResponse checkConversation(List<Message> messages)
GuardrailResponse checkConversation(List<Message> messages, String model)
```

**参数:**
- `messages` (List<Message>): 对话消息列表
- `model` (String, 可选): 模型名称

##### healthCheck()

检查API服务健康状态。

```java
Map<String, Object> healthCheck()
```

##### getModels()

获取可用模型列表。

```java
Map<String, Object> getModels()
```

### AsyncXiangxinAIClient（异步客户端，推荐）

#### 构造函数

```java
// 使用默认配置
AsyncXiangxinAIClient asyncClient = new AsyncXiangxinAIClient("your-api-key");

// 自定义配置
AsyncXiangxinAIClient asyncClient = new AsyncXiangxinAIClient(
    "your-api-key",     // API密钥
    "https://api.xiangxinai.cn/v1", // API基础URL
    30,                 // 请求超时时间（秒）
    3                   // 最大重试次数
);
```

#### 异步方法

##### checkPromptAsync(content)

异步检测单个提示词的安全性。

```java
CompletableFuture<GuardrailResponse> checkPromptAsync(String content)
CompletableFuture<GuardrailResponse> checkPromptAsync(String content, String model)
```

**返回值:**
- `CompletableFuture<GuardrailResponse>`: 异步检测结果

**示例:**
```java
// 方式1: 阻塞等待
CompletableFuture<GuardrailResponse> future = asyncClient.checkPromptAsync("用户问题");
GuardrailResponse result = future.get();

// 方式2: 回调处理（推荐）
asyncClient.checkPromptAsync("用户问题")
    .thenAccept(result -> {
        System.out.println("检测完成: " + result.getOverallRiskLevel());
    })
    .exceptionally(throwable -> {
        System.err.println("检测失败: " + throwable.getMessage());
        return null;
    });
```

##### checkConversationAsync(messages)

异步检测对话上下文的安全性。

```java
CompletableFuture<GuardrailResponse> checkConversationAsync(List<Message> messages)
CompletableFuture<GuardrailResponse> checkConversationAsync(List<Message> messages, String model)
```

##### healthCheckAsync()

异步检查API服务健康状态。

```java
CompletableFuture<Map<String, Object>> healthCheckAsync()
```

##### getModelsAsync()

异步获取可用模型列表。

```java
CompletableFuture<Map<String, Object>> getModelsAsync()
```

### 数据模型

#### Message

```java
public class Message {
    private String role;    // "user", "system", "assistant"
    private String content; // 消息内容
    
    public Message(String role, String content)
    // getter/setter方法...
}
```

#### GuardrailResponse

```java
public class GuardrailResponse {
    private String id;                    // 请求唯一标识
    private GuardrailResult result;       // 检测结果详情
    private String overallRiskLevel;      // 综合风险等级
    private String suggestAction;         // 建议动作
    private String suggestAnswer;         // 建议回答（可能为null）
    
    // 便捷方法
    public boolean isSafe()              // 判断是否安全
    public boolean isBlocked()           // 判断是否被阻断
    public boolean hasSubstitute()       // 判断是否有代答
    public List<String> getAllCategories() // 获取所有风险类别
}
```

#### GuardrailResult

```java
public class GuardrailResult {
    private ComplianceResult compliance;  // 合规检测结果
    private SecurityResult security;      // 安全检测结果
    private DataResult data;              // 数据防泄漏检测结果（v2.4.0新增）
}
```

#### ComplianceResult / SecurityResult / DataResult

```java
public class ComplianceResult {
    private String riskLevel;             // 风险等级
    private List<String> categories;      // 风险类别列表
}

public class DataResult {
    private String riskLevel;             // 风险等级
    private List<String> categories;      // 检测到的敏感数据类型（v2.4.0新增）
}
```

### 响应格式

```java
{
  "id": "guardrails-xxx",
  "result": {
    "compliance": {
      "risk_level": "无风险",           // 无风险/低风险/中风险/高风险
      "categories": []                  // 合规风险类别
    },
    "security": {
      "risk_level": "无风险",           // 无风险/低风险/中风险/高风险
      "categories": []                  // 安全风险类别
    },
    "data": {
      "risk_level": "无风险",           // 无风险/低风险/中风险/高风险（v2.4.0新增）
      "categories": []                  // 检测到的敏感数据类型（v2.4.0新增）
    }
  },
  "overall_risk_level": "无风险",       // 综合风险等级
  "suggest_action": "通过",             // 通过/阻断/代答
  "suggest_answer": null                // 建议回答（数据防泄漏时包含脱敏后内容）
}
```

## 异常处理

```java
import cn.xiangxinai.exception.*;

try {
    GuardrailResponse result = client.checkPrompt("test content");
    System.out.println(result);
} catch (AuthenticationException e) {
    System.err.println("认证失败，请检查API密钥: " + e.getMessage());
} catch (RateLimitException e) {
    System.err.println("请求频率过高，请稍后重试: " + e.getMessage());
} catch (ValidationException e) {
    System.err.println("输入参数无效: " + e.getMessage());
} catch (NetworkException e) {
    System.err.println("网络连接错误: " + e.getMessage());
} catch (XiangxinAIException e) {
    System.err.println("API错误: " + e.getMessage());
}
```

### 异常类型

- `XiangxinAIException` - 基础异常类
- `AuthenticationException` - 认证失败
- `RateLimitException` - 超出速率限制
- `ValidationException` - 输入验证错误
- `NetworkException` - 网络连接错误

## 使用场景

### 1. 内容审核

```java
// 用户生成内容检测
String userContent = "用户发布的内容...";
GuardrailResponse result = client.checkPrompt(userContent);

if (!result.isSafe()) {
    // 内容不安全，执行相应处理
    System.out.println("内容包含风险: " + result.getAllCategories());
}
```

### 2. 对话系统防护

```java
// AI对话系统中的安全检测
List<Message> conversation = Arrays.asList(
    new Message("user", "用户问题"),
    new Message("assistant", "准备发送给用户的回答")
);

GuardrailResponse result = client.checkConversation(conversation);

if ("代答".equals(result.getSuggestAction()) && result.getSuggestAnswer() != null) {
    // 使用安全的代答内容
    return result.getSuggestAnswer();
} else if (result.isBlocked()) {
    // 阻断不安全的对话
    return "抱歉，我无法回答这个问题";
}
```

### 3. 批量内容检测

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

### 4. Spring Boot 集成

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
            // 记录日志并返回保守的结果
            log.error("内容检测失败", e);
            return false;
        }
    }
}
```

## 最佳实践

1. **使用对话上下文检测**: 推荐使用 `checkConversation` 而不是 `checkPrompt`，因为上下文感知能提供更准确的检测结果。

2. **资源管理**: 使用 try-with-resources 或手动调用 `close()` 方法释放资源。

3. **异常处理**: 实现适当的异常处理和重试机制。

4. **连接复用**: 在应用中复用同一个 `XiangxinAIClient` 实例，避免频繁创建。

5. **配置调优**: 根据实际需要调整超时时间和重试次数。

6. **日志记录**: 记录检测结果用于分析和优化。

## 线程安全

`XiangxinAIClient` 是线程安全的，可以在多线程环境中共享使用。

## 许可证

Apache 2.0

## 技术支持

- 官网: https://xiangxinai.cn
- 文档: https://docs.xiangxinai.cn
- 问题反馈: https://github.com/xiangxinai/xiangxin-guardrails/issues
- 邮箱: wanglei@xiangxinai.cn