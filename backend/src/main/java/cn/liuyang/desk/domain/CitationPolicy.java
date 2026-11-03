package cn.liuyang.desk.domain;

import cn.liuyang.desk.dto.KnowledgeHit;
import java.util.*;

public final class CitationPolicy {
  private CitationPolicy() {}

  public static List<KnowledgeHit> selectSources(
      List<KnowledgeHit> candidates, List<String> ids, boolean fallback) {
    if (fallback) return candidates.stream().filter(x -> x.score() >= 0.12).limit(2).toList();
    Set<String> known = new HashSet<>();
    candidates.forEach(x -> known.add(x.id()));
    if (ids.stream().anyMatch(x -> !known.contains(x))) return List.of();
    return candidates.stream().filter(x -> ids.contains(x.id())).limit(3).toList();
  }
}
