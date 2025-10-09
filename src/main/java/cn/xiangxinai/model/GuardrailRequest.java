package cn.xiangxinai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Guardrail request model
 */
public class GuardrailRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private List<Message> messages;

    @JsonProperty("extra_body")
    private Map<String, Object> extraBody;

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

    public Map<String, Object> getExtraBody() {
        return extraBody;
    }

    public void setExtraBody(Map<String, Object> extraBody) {
        this.extraBody = extraBody;
    }

    @Override
    public String toString() {
        return "GuardrailRequest{" +
                "model='" + model + '\'' +
                ", messages=" + messages +
                ", extraBody=" + extraBody +
                '}';
    }
}