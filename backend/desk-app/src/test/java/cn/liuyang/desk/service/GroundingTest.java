package cn.liuyang.desk.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.liuyang.desk.ai.AiGateway;
import cn.liuyang.desk.domain.*;
import cn.liuyang.desk.dto.KnowledgeHit;
import java.util.*;
import org.junit.jupiter.api.Test;

class GroundingTest {
  @Test
  void cosineHandlesDimensionMismatch() {
    assertEquals(0, KnowledgeText.cosine(new float[] {1}, new float[] {1, 2}));
  }

  @Test
  void cosineHandlesZeroVector() {
    assertEquals(0, KnowledgeText.cosine(new float[] {0, 0}, new float[] {1, 2}));
  }

  @Test
  void cosineRecognizesSameDirection() {
    assertEquals(1, KnowledgeText.cosine(new float[] {1, 2}, new float[] {2, 4}), 0.00001);
  }

  @Test
  void splittingKeepsAllContent() {
    String body = "字".repeat(1800);
    var chunks = KnowledgeText.split(body);
    assertEquals(3, chunks.size());
    assertEquals(body, String.join("", chunks));
  }

  @Test
  void fallbackNeverInfersWrite() {
    assertEquals("KNOWLEDGE", AiGateway.fallback("请立即退款", "MODEL_UNAVAILABLE").intent());
  }

  @Test
  void fallbackExtractsExactOrderNumber() {
    var plan = AiGateway.fallback("查 ZS20261030001", "MODEL_UNAVAILABLE");
    assertEquals("ORDER", plan.intent());
    assertEquals("ZS20261030001", plan.orderNumber());
  }

  @Test
  void fabricatedCitationInvalidatesSelection() {
    var known = List.of(new KnowledgeHit("real", "标题", 1, 1, "原文", 0.9, "keyword"));
    assertTrue(CitationPolicy.selectSources(known, List.of("real", "invented"), false).isEmpty());
  }

  @Test
  void selectedCitationRemainsVerbatim() {
    var known = List.of(new KnowledgeHit("real", "标题", 1, 1, "七天内且包装完好", 0.9, "keyword"));
    assertEquals(
        "七天内且包装完好",
        CitationPolicy.selectSources(known, List.of("real"), false).getFirst().content());
  }
}
