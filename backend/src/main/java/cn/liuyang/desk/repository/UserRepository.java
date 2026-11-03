package cn.liuyang.desk.repository;

import cn.liuyang.desk.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepository {
  private final Store store;

  public List<User> findByUsername(String username) {
    return store.list("SELECT * FROM app_users WHERE username=?", User.class, username);
  }
}
