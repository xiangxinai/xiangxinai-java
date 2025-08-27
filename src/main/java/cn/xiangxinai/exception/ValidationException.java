package cn.xiangxinai.exception;

/**
 * 输入验证错误
 */
public class ValidationException extends XiangxinAIException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}