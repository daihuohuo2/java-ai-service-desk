package cn.liuyang.desk.converter;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import java.util.List;
import org.mapstruct.*;

/** MapStruct generates type-checked mapping code during compilation. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DeskConverter {
  ProductResponse product(Product source);

  Product productEntity(ProductResponse source);

  UserResponse user(User source);

  @Mapping(target = "events", ignore = true)
  TicketResponse ticket(Ticket source);

  TicketEventResponse event(TicketEvent source);

  List<TicketEventResponse> events(List<TicketEvent> source);

  @Mapping(target = "detail", ignore = true)
  ChatMessageResponse message(ChatMessage source);
}
