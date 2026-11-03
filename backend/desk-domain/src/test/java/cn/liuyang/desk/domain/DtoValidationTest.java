package cn.liuyang.desk.domain;

import static org.junit.jupiter.api.Assertions.*;
import cn.liuyang.desk.common.validation.EnumValue;
import cn.liuyang.desk.common.validation.ValidationGroups;
import cn.liuyang.desk.dto.ProductInput;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class DtoValidationTest {
  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

  @AfterAll static void close() { FACTORY.close(); }

  @Test void createRejectsClientAssignedId() {
    var input = new ProductInput("灯", "照明", 100, "商品说明", true, "client-id");
    assertFalse(FACTORY.getValidator().validate(input, ValidationGroups.Create.class).isEmpty());
  }

  @Test void updateRequiresIdAndStillChecksCommonFields() {
    var invalid = new ProductInput("", "照明", -1, "商品说明", true);
    var errors = FACTORY.getValidator().validate(invalid, ValidationGroups.Update.class);
    assertTrue(errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("id")));
    assertTrue(errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("name")));
    assertTrue(errors.stream().anyMatch(e -> e.getPropertyPath().toString().equals("priceCents")));
  }

  @Test void validOperationsUseDifferentGroups() {
    assertTrue(FACTORY.getValidator().validate(new ProductInput("灯", "照明", 100, "说明", true), ValidationGroups.Create.class).isEmpty());
    assertTrue(FACTORY.getValidator().validate(new ProductInput("灯", "照明", 100, "说明", true, "P100"), ValidationGroups.Update.class).isEmpty());
  }

  record KindInput(@NotBlank @EnumValue(enumClass = TicketKind.class) String kind) {}

  @Test void customConstraintRejectsUnknownAndBlankKinds() {
    var validator = FACTORY.getValidator();
    assertTrue(validator.validate(new KindInput("REFUND")).isEmpty());
    assertFalse(validator.validate(new KindInput("REFUND_NOW")).isEmpty());
    assertFalse(validator.validate(new KindInput(null)).isEmpty());
  }
}
