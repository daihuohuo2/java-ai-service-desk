package cn.liuyang.desk;

public record IndexJob(String id, String documentId, String state, int attempts, int indexed,
        int total, String error, String createdAt, String updatedAt) {
}
