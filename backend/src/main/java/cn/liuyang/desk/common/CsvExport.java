package cn.liuyang.desk.common;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CsvExport {
  private CsvExport() {}

  public static String encode(List<? extends List<?>> rows) {
    // UTF-8 BOM helps spreadsheet applications recognize Chinese text.
    return "\uFEFF"
        + rows.stream()
            .map(row -> row.stream().map(CsvExport::cell).collect(Collectors.joining(",")))
            .collect(Collectors.joining("\r\n"))
        + "\r\n";
  }

  public static String cell(Object value) {
    String text = Objects.toString(value, "");
    String leading = text.stripLeading();
    if (!leading.isEmpty() && "=+-@".indexOf(leading.charAt(0)) >= 0
        || text.startsWith("\t")
        || text.startsWith("\r")
        || text.startsWith("\n")) text = "'" + text;
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }
}
