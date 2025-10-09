package cn.xiangxinai;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xiangxinai.model.GuardrailResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ScoreFieldTest {

    @Test
    public void testScoreFieldDeserialization() {
        // 模拟包含score字段的API响应
        String jsonResponse = "{\"id\":\"guardrails-8d45c2cf9e014952ab1ce7584673b556\",\"result\":{\"compliance\":{\"risk_level\":\"medium_risk\",\"categories\":[\"Illegal Activities\"]},\"security\":{\"risk_level\":\"no_risk\",\"categories\":[]},\"data\":{\"risk_level\":\"no_risk\",\"categories\":[]}},\"overall_risk_level\":\"medium_risk\",\"suggest_action\":\"replace\",\"suggest_answer\":\"很抱歉，我不能提供涉及违法犯罪的信息。\",\"score\":0.9999984502816872}";
        
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            GuardrailResponse response = mapper.readValue(jsonResponse, GuardrailResponse.class);
            
            // 验证基本字段
            assertEquals("guardrails-8d45c2cf9e014952ab1ce7584673b556", response.getId());
            assertEquals("medium_risk", response.getOverallRiskLevel());
            assertEquals("replace", response.getSuggestAction());
            assertEquals("很抱歉，我不能提供涉及违法犯罪的信息。", response.getSuggestAnswer());
            
            // 验证score字段
            assertNotNull(response.getScore());
            assertEquals(0.9999984502816872, response.getScore(), 0.0001);
            
            System.out.println("✅ Java SDK 成功解析包含score字段的响应");
            System.out.println("Score值: " + response.getScore());
            
        } catch (Exception e) {
            fail("解析失败: " + e.getMessage());
        }
    }
    
    @Test
    public void testScoreFieldOptional() {
        // 测试不包含score字段的响应
        String jsonResponse = "{\"id\":\"test-id\",\"result\":{\"compliance\":{\"risk_level\":\"no_risk\",\"categories\":[]},\"security\":{\"risk_level\":\"no_risk\",\"categories\":[]}},\"overall_risk_level\":\"no_risk\",\"suggest_action\":\"pass\"}";
        
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            GuardrailResponse response = mapper.readValue(jsonResponse, GuardrailResponse.class);
            
            assertEquals("test-id", response.getId());
            assertEquals("no_risk", response.getOverallRiskLevel());
            assertEquals("pass", response.getSuggestAction());
            
            // score字段应该为null
            assertNull(response.getScore());
            
            System.out.println("✅ Java SDK 成功解析不包含score字段的响应");
            
        } catch (Exception e) {
            fail("解析失败: " + e.getMessage());
        }
    }
}
