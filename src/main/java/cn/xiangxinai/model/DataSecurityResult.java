package cn.xiangxinai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 数据安全检测结果
 */
public class DataSecurityResult {

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("categories")
    private List<String> categories;

    public DataSecurityResult() {
    }

    public DataSecurityResult(String riskLevel, List<String> categories) {
        this.riskLevel = riskLevel;
        this.categories = categories;
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
        this.categories = categories;
    }

    @Override
    public String toString() {
        return "DataSecurityResult{" +
                "riskLevel='" + riskLevel + '\'' +
                ", categories=" + categories +
                '}';
    }
}
