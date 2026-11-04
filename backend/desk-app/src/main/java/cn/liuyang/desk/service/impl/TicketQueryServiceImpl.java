package cn.liuyang.desk.service.impl;

import cn.liuyang.desk.common.PageResponse;
import cn.liuyang.desk.dto.TicketSummary;
import cn.liuyang.desk.repository.TicketSearchRepository;
import cn.liuyang.desk.service.TicketQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketQueryServiceImpl implements TicketQueryService {
  private final TicketSearchRepository repository;

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PageResponse<TicketSummary> page(
      String owner, boolean staff, int page, int size, String status, String query) {
    return repository.page(owner, staff, page, size, status, query);
  }

  public String export(String owner, boolean staff, String status, String query) {
    return repository.export(owner, staff, status, query);
  }
}
