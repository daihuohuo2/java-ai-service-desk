package cn.liuyang.desk.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatAnswer {
  private String runId;
  private String mode;
  private String promptVersion;
  private String intent;
  private String tool;
  private String answer;
  private java.util.List<KnowledgeHit> citations;
  private java.util.List<OrderResponse> orders;
  private cn.liuyang.desk.entity.Proposal proposal;
  private long latencyMs;
}
