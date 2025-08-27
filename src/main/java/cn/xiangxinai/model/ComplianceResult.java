package cn.xiangxinai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * 合规检测结果
 */
public class ComplianceResult {
    
    @JsonProperty("risk_level")
    private String riskLevel;
    
    @JsonProperty("categories")
    private List<String> categories;
    
    public ComplianceResult() {
        this.categories = new ArrayList<>();
    }
    
    public ComplianceResult(String riskLevel, List<String> categories) {
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
        return "ComplianceResult{" +
                "riskLevel='" + riskLevel + '\'' +
                ", categories=" + categories +
                '}';
    }
}