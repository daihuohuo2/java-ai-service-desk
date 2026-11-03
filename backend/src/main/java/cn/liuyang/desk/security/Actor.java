package cn.liuyang.desk.security;

import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.repository.Store;
import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Actor {
  private final Store s;

  public Actor(Store s) {
    this.s = s;
  }

  public Map<String, Object> current() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
      throw new ApiException(401, "请先登录");
    return s.one(
        "SELECT id,username,display_name,role FROM app_users WHERE username=?", auth.getName());
  }

  public String id() {
    return (String) current().get("id");
  }

  public boolean staff() {
    return !"CUSTOMER".equals(current().get("role"));
  }
}
