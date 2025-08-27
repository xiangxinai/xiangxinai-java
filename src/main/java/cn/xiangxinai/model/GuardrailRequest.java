package cn.xiangxinai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 护栏检测请求模型
 */
public class GuardrailRequest {
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("messages")
    private List<Message> messages;
    
    public GuardrailRequest() {
    }
    
    public GuardrailRequest(String model, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages cannot be empty");
        }
        this.model = model;
        this.messages = messages;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        if (messages != null && messages.isEmpty()) {
            throw new IllegalArgumentException("messages cannot be empty");
        }
        this.messages = messages;
    }
    
    @Override
    public String toString() {
        return "GuardrailRequest{" +
                "model='" + model + '\'' +
                ", messages=" + messages +
                '}';
    }
}