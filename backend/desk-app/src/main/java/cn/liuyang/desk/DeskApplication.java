package cn.liuyang.desk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude =
        org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class)
@org.springframework.scheduling.annotation.EnableScheduling
public class DeskApplication {
  public static void main(String[] args) {
    SpringApplication.run(DeskApplication.class, args);
  }
}
