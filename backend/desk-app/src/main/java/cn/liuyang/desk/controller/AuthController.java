package cn.liuyang.desk.controller;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.User;
import cn.liuyang.desk.repository.Store;
import cn.liuyang.desk.security.Actor;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final Store store;
  private final Actor actor;
  private final HttpSessionCsrfTokenRepository csrf;
  private final cn.liuyang.desk.service.AuthService auth;

  public AuthController(
      Store store,
      Actor actor,
      HttpSessionCsrfTokenRepository csrf,
      cn.liuyang.desk.service.AuthService auth) {
    this.store = store;
    this.actor = actor;
    this.csrf = csrf;
    this.auth = auth;
  }

  record Login(
      @NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 100) String password) {}

  @GetMapping("/auth/csrf")
  CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getToken());
  }

  @GetMapping("/auth/me")
  Object me() {
    return actor.current();
  }

  @PostMapping("/auth/login")
  Object login(
      @Valid @RequestBody Login input, HttpServletRequest request, HttpServletResponse response) {
    User user = auth.authenticate(input.username(), input.password(), request.getRemoteAddr());
    request.getSession();
    request.changeSessionId();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
    SecurityContextHolder.setContext(context);
    new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    csrf.saveToken(null, request, response);
    store.audit(user.getId(), "LOGIN", null, "用户登录");
    return actor.current();
  }

  @GetMapping("/meta")
  Object meta(@Value("${desk.demo}") boolean demo) {
    return new MetaResponse("知序客服工作台", "1.0.0", demo);
  }
}
