package cn.liuyang.desk.controller;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.service.AdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
  private final AdminService service;

  @GetMapping("/overview")
  public OverviewResponse overview() {
    return service.overview();
  }

  @GetMapping("/audit")
  public List<AuditResponse> audit() {
    return service.audit();
  }
}
