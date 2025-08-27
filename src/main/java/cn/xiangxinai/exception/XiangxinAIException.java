package cn.xiangxinai.exception;

/**
 * 象信AI安全护栏基础异常类
 */
public class XiangxinAIException extends RuntimeException {
    
    public XiangxinAIException(String message) {
        super(message);
    }
    
    public XiangxinAIException(String message, Throwable cause) {
        super(message, cause);
    }
}