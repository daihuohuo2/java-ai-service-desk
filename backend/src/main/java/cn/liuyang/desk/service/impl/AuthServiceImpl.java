package cn.liuyang.desk.service.impl;

import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.entity.User;
import cn.liuyang.desk.repository.UserRepository;
import cn.liuyang.desk.service.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
  private final UserRepository usersRepository;
  private final PasswordEncoder passwords;
  private final ConcurrentHashMap<String, long[]> failures = new ConcurrentHashMap<>();
  private final String dummy;

  public AuthServiceImpl(UserRepository usersRepository, PasswordEncoder passwords) {
    this.usersRepository = usersRepository;
    this.passwords = passwords;
    this.dummy = passwords.encode("timing-mitigation-only");
  }

  public User authenticate(String username, String password, String remoteAddress) {
    String key = remoteAddress + ":" + username;
    long time = System.currentTimeMillis();
    synchronized (failures) {
      failures.entrySet().removeIf(e -> time - e.getValue()[1] > 60000);
      var previous = failures.get(key);
      if (previous != null && previous[0] >= 8) throw new ApiException(429, "登录尝试过多，请一分钟后重试");
      if (failures.size() > 10000) throw new ApiException(429, "登录繁忙，请稍后重试");
    }
    var users = usersRepository.findByUsername(username);
    boolean valid =
        passwords.matches(password, users.isEmpty() ? dummy : users.getFirst().getPasswordHash());
    if (!valid || users.isEmpty()) {
      synchronized (failures) {
        failures.compute(key, (k, v) -> new long[] {v == null ? 1 : v[0] + 1, time});
      }
      throw new ApiException(401, "用户名或密码错误");
    }
    failures.remove(key);
    return users.getFirst();
  }
}
