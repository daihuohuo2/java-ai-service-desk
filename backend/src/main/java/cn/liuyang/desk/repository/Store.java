package cn.liuyang.desk.repository;

import cn.liuyang.desk.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class Store {
  public final JdbcTemplate db;
  public final ObjectMapper json;

  public Store(JdbcTemplate db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  public static String id() {
    return UUID.randomUUID().toString();
  }

  public static String now() {
    return Instant.now().toString();
  }

  public Map<String, Object> one(String sql, Object... args) {
    var rows = db.queryForList(sql, args);
    if (rows.isEmpty()) throw ApiException.missing();
    return rows.getFirst();
  }

  public String encode(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> decode(String s) {
    try {
      return json.readValue(s, Map.class);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static String hash(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public void audit(String actor, String action, String target, String summary) {
    db.update(
        "INSERT INTO audit_log VALUES(?,?,?,?,?,?)", id(), actor, action, target, summary, now());
  }
}
