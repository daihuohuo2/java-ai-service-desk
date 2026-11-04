package cn.liuyang.desk.domain;

import cn.liuyang.desk.common.ApiException;
import java.util.*;

public final class KnowledgeText {
  private KnowledgeText() {}

  public static List<String> split(String input) {
    var result = new ArrayList<String>();
    for (String part : input.replace("\r\n", "\n").split("\n\\s*\n")) {
      part = part.strip();
      for (int i = 0; i < part.length(); i += 600)
        result.add(part.substring(i, Math.min(i + 600, part.length())));
    }
    if (result.size() > 60) {
      throw new ApiException(400, "单份资料最多 60 个片段");
    }
    return result;
  }

  public static Set<String> grams(String s) {
    s = s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    var result = new HashSet<String>();
    for (int i = 0; i < s.length() - 1; i++) result.add(s.substring(i, i + 2));
    return result;
  }

  public static double lexical(String a, String b) {
    var x = grams(a);
    var y = grams(b);
    if (x.isEmpty()) {
      return 0;
    }
    long intersection = x.stream().filter(y::contains).count();
    return (double) intersection / x.size();
  }

  public static double cosine(float[] a, float[] b) {
    if (a.length != b.length) {
      return 0;
    }
    double sum = 0, na = 0, nb = 0;
    for (int i = 0; i < a.length; i++) {
      sum += a[i] * b[i];
      na += a[i] * a[i];
      nb += b[i] * b[i];
    }
    return na == 0 || nb == 0 ? 0 : sum / Math.sqrt(na * nb);
  }
}
