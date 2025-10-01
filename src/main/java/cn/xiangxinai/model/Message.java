package cn.xiangxinai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 消息模型
 */
public class Message {

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private Object content;  // 可以是String或List<Object>（多模态）

    public Message() {
    }

    public Message(String role, String content) {
        if (role == null || (!role.equals("user") && !role.equals("system") && !role.equals("assistant"))) {
            throw new IllegalArgumentException("role must be one of: user, system, assistant");
        }
        if (content != null && content.length() > 1000000) {
            throw new IllegalArgumentException("content too long (max 1000000 characters)");
        }

        this.role = role;
        this.content = content != null ? content.trim() : content;
    }

    public Message(String role, Object content) {
        if (role == null || (!role.equals("user") && !role.equals("system") && !role.equals("assistant"))) {
            throw new IllegalArgumentException("role must be one of: user, system, assistant");
        }

        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if (role != null && !role.equals("user") && !role.equals("system") && !role.equals("assistant")) {
            throw new IllegalArgumentException("role must be one of: user, system, assistant");
        }
        this.role = role;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public void setContent(String content) {
        if (content != null && content.length() > 1000000) {
            throw new IllegalArgumentException("content too long (max 1000000 characters)");
        }
        this.content = content != null ? content.trim() : content;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}