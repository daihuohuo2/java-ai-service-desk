package cn.liuyang.desk.common.validation;

import jakarta.validation.groups.Default;

/** Extending Default preserves common constraints when a specific operation is validated. */
public final class ValidationGroups {
  private ValidationGroups() {}
  public interface Create extends Default {}
  public interface Update extends Default {}
}
