package cn.liuyang.desk.service;

import cn.liuyang.desk.dto.*;
import java.util.List;

/** Business contract; see service.impl.AdminServiceImpl for execution. */
public interface AdminService {
  OverviewResponse overview();

  List<AuditResponse> audit();
}
