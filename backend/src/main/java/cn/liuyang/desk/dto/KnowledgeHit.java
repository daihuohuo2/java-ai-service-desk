package cn.liuyang.desk.dto;

public record KnowledgeHit(
    String id,
    String title,
    int version,
    int ordinal,
    String content,
    double score,
    String retrieval) {}
