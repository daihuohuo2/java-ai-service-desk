package cn.liuyang.desk.dto;

public record OverviewResponse(
    java.util.List<TicketCount> tickets,
    int orders,
    int knowledge,
    int modelRuns,
    java.util.List<RunSummary> recentRuns) {}
