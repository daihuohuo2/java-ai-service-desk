package cn.liuyang.desk.mapper;

import cn.liuyang.desk.dto.OrderResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** MyBatis binds values with #{...}; user input never becomes SQL syntax. */
@Mapper
public interface OrderMapper {
  String COLUMNS =
      "o.id,o.order_no,o.owner_id,o.product_id,o.quantity,o.total_cents,"
          + "o.status,o.created_at,o.delivered_at,p.name product_name,p.category";
  String FROM = " FROM shop_orders o JOIN products p ON o.product_id=p.id ";
  String FILTER =
      " WHERE o.owner_id=#{owner} "
          + "<if test='status != null'> AND o.status=#{status} </if>"
          + "<if test='query != null'> AND o.order_no LIKE #{query} ESCAPE '!' </if>";

  @Select("SELECT " + COLUMNS + FROM + "WHERE o.owner_id=#{owner} ORDER BY o.created_at DESC,o.id")
  List<OrderResponse> list(String owner);

  @Select(
      "SELECT " + COLUMNS + FROM + "WHERE o.owner_id=#{owner} AND (o.id=#{id} OR o.order_no=#{id})")
  OrderResponse owned(@Param("owner") String owner, @Param("id") String id);

  @Select(
      "<script>SELECT "
          + COLUMNS
          + FROM
          + FILTER
          + " ORDER BY o.created_at DESC,o.id LIMIT #{size} OFFSET #{offset}</script>")
  List<OrderResponse> page(
      @Param("owner") String owner,
      @Param("status") String status,
      @Param("query") String query,
      @Param("size") int size,
      @Param("offset") long offset);

  @Select("<script>SELECT COUNT(*) " + FROM + FILTER + "</script>")
  long count(
      @Param("owner") String owner, @Param("status") String status, @Param("query") String query);
}
