package cn.liuyang.desk.security;

import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.dto.UserResponse;
import cn.liuyang.desk.entity.User;
import cn.liuyang.desk.repository.Store;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Actor {
  private final Store s;

  public Actor(Store s) {
    this.s = s;
  }

  public UserResponse current() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
      throw new ApiException(401, "请先登录");
    User user =
        s.one(
            "SELECT id,username,display_name,role FROM app_users WHERE username=?",
            User.class,
            auth.getName());
    return new UserResponse(
        user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
  }

  public String id() {
    return current().id();
  }

  public boolean staff() {
    return !"CUSTOMER".equals(current().role());
  }
}
