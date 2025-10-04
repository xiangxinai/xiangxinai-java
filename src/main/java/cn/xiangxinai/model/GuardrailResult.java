package cn.xiangxinai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 护栏检测结果
 */
public class GuardrailResult {

    @JsonProperty("compliance")
    private ComplianceResult compliance;

    @JsonProperty("security")
    private SecurityResult security;

    @JsonProperty("data")
    private DataSecurityResult data;

    public GuardrailResult() {
    }

    public GuardrailResult(ComplianceResult compliance, SecurityResult security) {
        this.compliance = compliance;
        this.security = security;
    }

    public GuardrailResult(ComplianceResult compliance, SecurityResult security, DataSecurityResult data) {
        this.compliance = compliance;
        this.security = security;
        this.data = data;
    }

    public ComplianceResult getCompliance() {
        return compliance;
    }

    public void setCompliance(ComplianceResult compliance) {
        this.compliance = compliance;
    }

    public SecurityResult getSecurity() {
        return security;
    }

    public void setSecurity(SecurityResult security) {
        this.security = security;
    }

    public DataSecurityResult getData() {
        return data;
    }

    public void setData(DataSecurityResult data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "GuardrailResult{" +
                "compliance=" + compliance +
                ", security=" + security +
                ", data=" + data +
                '}';
    }
}