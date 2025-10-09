package cn.xiangxinai.exception;

/**
 * Authentication error
 */
public class AuthenticationException extends XiangxinAIException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}