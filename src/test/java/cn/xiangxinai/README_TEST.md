# 象信AI安全护栏 Java SDK 测试指南

## 快速开始

### 1. 环境要求

- Java 11 或更高版本
- Maven 3.6 或更高版本

### 2. 设置API密钥

```bash
export XIANGXINAI_API_KEY="your-api-key-here"
```

### 3. 运行测试

```bash
mvn test
```

## 测试命令

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=XiangxinAIClientTest

# 运行特定测试方法
mvn test -Dtest=XiangxinAIClientTest#testCheckPromptSafeContent

# 运行测试并生成报告
mvn test surefire-report:report

# 生成测试覆盖率报告
mvn test jacoco:report

# 运行测试时显示详细输出
mvn test -X

# 跳过测试
mvn install -DskipTests
```

## Maven配置

确保 `pom.xml` 包含以下测试依赖：

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.9.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Surefire Plugin -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.0.0-M9</version>
        </plugin>
        
        <!-- JaCoCo Plugin -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.8</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## 测试用例说明

### 基本功能测试
- `testCheckPromptSafeContent` - 测试安全内容检测
- `testCheckPromptEmptyContent` - 测试空内容处理
- `testCheckPromptWithUserId` - 测试带用户ID的检测
- `testCheckConversation` - 测试对话上下文检测
- `testCheckResponseCtx` - 测试上下文感知输出检测
- `testHealthCheck` - 测试健康检查
- `testGetModels` - 测试获取模型列表

### 多模态功能测试
- `testCheckPromptImage` - 测试单张图片检测
- `testCheckPromptImageEmptyPrompt` - 测试空提示词的图片检测
- `testCheckPromptImages` - 测试多张图片检测

### 错误处理测试
- `testCheckConversationEmptyMessages` - 测试空消息列表错误
- `testCheckConversationNullMessage` - 测试null消息错误
- `testCheckPromptImageInvalidPath` - 测试无效图片路径错误
- `testCheckPromptImagesEmptyList` - 测试空图片列表错误
- `testInvalidApiKey` - 测试无效API密钥错误
- `testEmptyApiKey` - 测试空API密钥错误

### 配置测试
- `testCustomConfig` - 测试自定义配置

### 响应辅助方法测试
- `testResponseHelpers` - 测试响应辅助方法

### 性能和并发测试
- `testConcurrentRequests` - 测试并发请求处理
- `testPerformanceBenchmark` - 测试性能基准

### 集成测试
- `testCompleteWorkflow` - 测试完整工作流程

## 测试注解说明

- `@Test` - 标记测试方法
- `@Order(n)` - 指定测试执行顺序
- `@DisplayName("描述")` - 测试方法显示名称
- `@EnabledIfEnvironmentVariable` - 仅在环境变量存在时运行
- `@BeforeAll` - 在所有测试前执行
- `@AfterAll` - 在所有测试后执行

## 示例输出

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running cn.xiangxinai.XiangxinAIClientTest
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.345 s - in XiangxinAIClientTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## 测试报告

测试完成后，可以在以下位置查看报告：

- Surefire报告：`target/surefire-reports/`
- JaCoCo覆盖率报告：`target/site/jacoco/index.html`

## 注意事项

1. 如果没有设置 `XIANGXINAI_API_KEY`，集成测试将被跳过
2. 测试使用JUnit 5框架
3. 测试需要网络连接访问象信AI API
4. 图片测试使用公开的测试图片URL
5. 测试按照 `@Order` 注解指定的顺序执行
6. 使用 `try-with-resources` 语句确保客户端资源正确释放
7. 并发测试使用 `CompletableFuture` 和线程池
8. 性能测试会输出响应时间到控制台
