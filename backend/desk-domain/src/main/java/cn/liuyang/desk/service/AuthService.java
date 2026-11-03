package cn.liuyang.desk.service;

import cn.liuyang.desk.entity.User;

/** Business contract; see service.impl.AuthServiceImpl for execution. */
public interface AuthService {
  User authenticate(String username, String password, String remoteAddress);
}
