package cn.liuyang.desk.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.liuyang.desk.domain.CitationPolicy;
import cn.liuyang.desk.dto.KnowledgeHit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import tools.jackson.databind.json.JsonMapper;

class AiGatewayTest {
  private final ChatModel chat = mock(ChatModel.class);
  private final JsonMapper json = JsonMapper.builder().build();
  private final List<KnowledgeHit> candidates =
      List.of(
          new KnowledgeHit(
              "b1e0de8a-5c6a-4b51-a074-4323dc39e99c", "规格", 1, 1, "不含内置电池", 1.32, "keyword"),
          new KnowledgeHit(
              "8034969d-3f19-4856-97a7-38de372f400d", "规格", 1, 2, "无 App 连接功能", 1.08, "keyword"));
  private AiGateway gateway;

  @BeforeEach
  void createGateway() {
    gateway = new AiGateway(chat, mock(EmbeddingModel.class), json);
    gateway.enabled = true;
    gateway.timeout = 3;
  }

  private void modelReturnsReferences(String... references) {
    modelReturns(
        json.writeValueAsString(
            Map.of(
                "intent",
                "KNOWLEDGE",
                "orderNumber",
                "",
                "kind",
                "QUESTION",
                "sourceIds",
                references)));
  }

  private void modelReturns(String body) {
    when(chat.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(body)))));
  }

  @Test
  void shortReferencesResolveToOriginalIdsWithoutMutatingCandidates() {
    modelReturnsReferences("S2");
    var plan = gateway.plan("可以连接 App 吗？", List.of(), candidates);
    assertEquals("OLLAMA", plan.mode());
    assertEquals(List.of(candidates.get(1).id()), plan.sourceIds());
    assertEquals(
        candidates.get(1),
        CitationPolicy.selectSources(candidates, plan.sourceIds(), false).getFirst());

    var capture = ArgumentCaptor.forClass(Prompt.class);
    verify(chat).call(capture.capture());
    var input = json.readTree(capture.getValue().getUserMessage().getText());
    assertEquals("S1", input.path("candidates").get(0).path("id").asString());
    assertEquals("S2", input.path("candidates").get(1).path("id").asString());
    assertEquals("8034969d-3f19-4856-97a7-38de372f400d", candidates.get(1).id());
  }

  @Test
  void unknownReferenceRejectsAllCitationsIncludingAnOtherwiseValidOne() {
    modelReturnsReferences("S1", "S999");
    var plan = gateway.plan("产品参数", List.of(), candidates);
    assertEquals("OLLAMA", plan.mode());
    assertTrue(CitationPolicy.selectSources(candidates, plan.sourceIds(), false).isEmpty());
  }

  @Test
  void referencesCannotResolveAgainstCandidatesFromAnEarlierRequest() {
    modelReturnsReferences("S2");
    assertEquals(
        List.of(candidates.get(1).id()), gateway.plan("第一次", List.of(), candidates).sourceIds());
    var currentCandidates = List.of(candidates.getFirst());
    var plan = gateway.plan("第二次", List.of(), currentCandidates);
    assertTrue(CitationPolicy.selectSources(currentCandidates, plan.sourceIds(), false).isEmpty());
  }

  @Test
  void rawDatabaseIdCannotBypassTheRequestReferenceTable() {
    modelReturnsReferences((String) candidates.getFirst().id());
    var plan = gateway.plan("产品参数", List.of(), candidates);
    assertTrue(CitationPolicy.selectSources(candidates, plan.sourceIds(), false).isEmpty());
  }

  @Test
  void malformedModelJsonFallsBackToReadOnlyBehavior() {
    modelReturns("not a JSON object");
    var plan = gateway.plan("请直接退款", List.of(), candidates);
    assertEquals("MODEL_UNAVAILABLE", plan.mode());
    assertEquals("KNOWLEDGE", plan.intent());
    assertEquals("QUESTION", plan.kind());
    assertTrue(plan.sourceIds().isEmpty());
  }
}
