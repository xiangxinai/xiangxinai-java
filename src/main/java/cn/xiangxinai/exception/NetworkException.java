package cn.xiangxinai.exception;

/**
 * Network exception
 */
public class NetworkException extends XiangxinAIException {
    
    public NetworkException(String message) {
        super(message);
    }
    
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}