package cn.liuyang.desk.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {
  private Set<String> allowed;

  @Override
  public void initialize(EnumValue annotation) {
    allowed =
        Arrays.stream(annotation.enumClass().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // Null handling belongs to @NotNull/@NotBlank so this constraint is reusable for optional
    // fields.
    return value == null || allowed.contains(value);
  }
}
