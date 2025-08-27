package cn.xiangxinai.exception;

/**
 * 认证错误
 */
public class AuthenticationException extends XiangxinAIException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}