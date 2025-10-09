package cn.xiangxinai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Guardrail response model
 */
public class GuardrailResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("result")
    private GuardrailResult result;
    
    @JsonProperty("overall_risk_level")
    private String overallRiskLevel;
    
    @JsonProperty("suggest_action")
    private String suggestAction;
    
    @JsonProperty("suggest_answer")
    private String suggestAnswer;
    
    public GuardrailResponse() {
    }
    
    public GuardrailResponse(String id, GuardrailResult result, String overallRiskLevel, 
                            String suggestAction, String suggestAnswer) {
        this.id = id;
        this.result = result;
        this.overallRiskLevel = overallRiskLevel;
        this.suggestAction = suggestAction;
        this.suggestAnswer = suggestAnswer;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public GuardrailResult getResult() {
        return result;
    }
    
    public void setResult(GuardrailResult result) {
        this.result = result;
    }
    
    public String getOverallRiskLevel() {
        return overallRiskLevel;
    }
    
    public void setOverallRiskLevel(String overallRiskLevel) {
        this.overallRiskLevel = overallRiskLevel;
    }
    
    public String getSuggestAction() {
        return suggestAction;
    }
    
    public void setSuggestAction(String suggestAction) {
        this.suggestAction = suggestAction;
    }
    
    public String getSuggestAnswer() {
        return suggestAnswer;
    }
    
    public void setSuggestAnswer(String suggestAnswer) {
        this.suggestAnswer = suggestAnswer;
    }
    
    /**
     * Check if the content is safe
     */
    public boolean isSafe() {
        return "pass".equals(suggestAction);
    }
    
    /**
     * Check if the content is blocked
     */
    public boolean isBlocked() {
        return "reject".equals(suggestAction);
    }
    
    /**
     * Check if there is a substitute
     */
    public boolean hasSubstitute() {
        return "replace".equals(suggestAction) || "reject".equals(suggestAction);
    }
    
    /**
     * Get all risk categories
     */
    public List<String> getAllCategories() {
        Set<String> categorySet = new HashSet<>();
        
        if (result != null) {
            if (result.getCompliance() != null && result.getCompliance().getCategories() != null) {
                categorySet.addAll(result.getCompliance().getCategories());
            }
            if (result.getSecurity() != null && result.getSecurity().getCategories() != null) {
                categorySet.addAll(result.getSecurity().getCategories());
            }
            if (result.getData() != null && result.getData().getCategories() != null) {
                categorySet.addAll(result.getData().getCategories());
            }
        }
        
        return new ArrayList<>(categorySet);
    }
    
    @Override
    public String toString() {
        return "GuardrailResponse{" +
                "id='" + id + '\'' +
                ", result=" + result +
                ", overallRiskLevel='" + overallRiskLevel + '\'' +
                ", suggestAction='" + suggestAction + '\'' +
                ", suggestAnswer='" + suggestAnswer + '\'' +
                '}';
    }
}