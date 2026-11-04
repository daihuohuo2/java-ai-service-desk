package cn.liuyang.desk.dto;

public record PromptInput(
    String question, java.util.List<String> history, java.util.List<KnowledgeHit> candidates) {}
