package cn.liuyang.desk.service.impl;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.repository.AdminRepository;
import cn.liuyang.desk.service.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
  private final AdminRepository repository;

  public OverviewResponse overview() {
    return repository.overview();
  }

  public List<AuditResponse> audit() {
    return repository.audit();
  }
}
