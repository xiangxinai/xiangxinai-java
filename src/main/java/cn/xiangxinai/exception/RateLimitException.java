package cn.xiangxinai.exception;

/**
 * Rate limit exception
 */
public class RateLimitException extends XiangxinAIException {
    
    public RateLimitException(String message) {
        super(message);
    }
    
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}