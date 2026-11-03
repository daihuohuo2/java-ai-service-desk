package cn.liuyang.desk.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  HttpSessionCsrfTokenRepository csrfRepository() {
    return new HttpSessionCsrfTokenRepository();
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      HttpSessionCsrfTokenRepository csrf,
      @Value("${desk.allowed-origin}") String origin)
      throws Exception {
    var cors = new CorsConfiguration();
    cors.setAllowedOrigins(java.util.Arrays.stream(origin.split(",")).map(String::trim).toList());
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN", "Idempotency-Key"));
    cors.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    http.cors(c -> c.configurationSource(source))
        .csrf(
            c ->
                c.csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/auth/csrf",
                        "/api/auth/login",
                        "/api/meta",
                        "/actuator/health",
                        "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers(
                        "/api/knowledge/**", "/api/admin/audit", "/api/admin/products/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/admin/**")
                    .hasAnyRole("ADMIN", "AGENT")
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/json;charset=UTF-8");
                          res.getWriter().write("{\"code\":401,\"message\":\"请先登录\",\"data\":null}");
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/json;charset=UTF-8");
                          res.getWriter().write("{\"code\":403,\"message\":\"权限不足或会话校验失败，请刷新后重试\",\"data\":null}");
                        }))
        .sessionManagement(c -> c.sessionFixation(f -> f.changeSessionId()))
        .logout(
            c ->
                c.logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("DESK_SESSION")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .headers(
            c ->
                c.contentSecurityPolicy(
                    p -> p.policyDirectives("default-src 'none'; frame-ancestors 'none'")));
    return http.build();
  }
}
