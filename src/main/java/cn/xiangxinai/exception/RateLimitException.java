package cn.xiangxinai.exception;

/**
 * 速率限制错误
 */
public class RateLimitException extends XiangxinAIException {
    
    public RateLimitException(String message) {
        super(message);
    }
    
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}