package cn.xiangxinai.exception;

/**
 * 网络错误
 */
public class NetworkException extends XiangxinAIException {
    
    public NetworkException(String message) {
        super(message);
    }
    
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}