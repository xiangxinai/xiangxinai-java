package cn.xiangxinai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Security result
 */
public class SecurityResult {
    
    @JsonProperty("risk_level")
    private String riskLevel;
    
    @JsonProperty("categories")
    private List<String> categories;
    
    public SecurityResult() {
        this.categories = new ArrayList<>();
    }
    
    public SecurityResult(String riskLevel, List<String> categories) {
        this.riskLevel = riskLevel;
        this.categories = categories != null ? categories : new ArrayList<>();
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public List<String> getCategories() {
        return categories;
    }
    
    public void setCategories(List<String> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }
    
    @Override
    public String toString() {
        return "SecurityResult{" +
                "riskLevel='" + riskLevel + '\'' +
                ", categories=" + categories +
                '}';
    }
}