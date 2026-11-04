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

  public String encode(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public <T> T decode(String source, Class<T> type) {
    return json.readValue(source, type);
  }

  public <T> List<T> list(String sql, Class<T> type, Object... args) {
    return db.query(
        sql, org.springframework.jdbc.core.BeanPropertyRowMapper.newInstance(type), args);
  }

  public <T> T one(String sql, Class<T> type, Object... args) {
    return list(sql, type, args).stream().findFirst().orElseThrow(ApiException::missing);
  }

  public void require(String sql, Object... args) {
    if (db.queryForList(sql, String.class, args).isEmpty()) {
      throw ApiException.missing();
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
