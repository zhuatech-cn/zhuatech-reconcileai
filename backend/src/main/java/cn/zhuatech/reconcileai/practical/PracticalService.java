/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.reconcileai.practical;

import static cn.zhuatech.reconcileai.practical.PracticalInput.*;

import java.math.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
@Service
public class PracticalService {
  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  public Map<String, Object> execute(Map<String, Object> r) {
    validate(r);
    keys(r, "dateToleranceDays", "left", "right");
    int tolerance = integer(r, "dateToleranceDays", 0, 31);
    var left = rows(r, "left", 0, 300);
    var right = rows(r, "right", 0, 300);
    for (var side : List.of(left, right)) {
      Set<String> ids = new HashSet<>();
      for (var row : side) {
        keys(row, "id", "reference", "amount", "date");
        unique(ids, id(row, "id"));
        text(row, "reference", 100);
        money(row, "amount");
        date(text(row, "date", 10));
      }
    }
    Map<String, List<String>> l = new TreeMap<>(), rr = new TreeMap<>();
    for (var row : left) l.put(row.get("id").toString(), new ArrayList<>());
    for (var row : right) rr.put(row.get("id").toString(), new ArrayList<>());
    for (var a : left)
      for (var b : right)
        if (money(a, "amount").signum() > 0
            && money(a, "amount").compareTo(money(b, "amount")) == 0
            && a.get("reference").toString().strip().equals(b.get("reference").toString().strip())
            && Math.abs(
                    java.time.temporal.ChronoUnit.DAYS.between(
                        date(a.get("date").toString()), date(b.get("date").toString())))
                <= tolerance) {
          l.get(a.get("id").toString()).add(b.get("id").toString());
          rr.get(b.get("id").toString()).add(a.get("id").toString());
        }
    List<Map<String, Object>> matches = new ArrayList<>(), exceptions = new ArrayList<>();
    Set<String> matchedRight = new HashSet<>();
    for (var e : l.entrySet()) {
      var candidates = e.getValue();
      if (candidates.size() == 1 && rr.get(candidates.getFirst()).size() == 1) {
        matches.add(Map.of("left", e.getKey(), "right", candidates.getFirst()));
        matchedRight.add(candidates.getFirst());
      } else
        exceptions.add(
            Map.of(
                "side",
                "LEFT",
                "id",
                e.getKey(),
                "state",
                candidates.isEmpty() ? "UNMATCHED" : "AMBIGUOUS",
                "candidates",
                candidates));
    }
    for (var e : rr.entrySet())
      if (!matchedRight.contains(e.getKey()))
        exceptions.add(
            Map.of(
                "side",
                "RIGHT",
                "id",
                e.getKey(),
                "state",
                e.getValue().isEmpty() ? "UNMATCHED" : "AMBIGUOUS",
                "candidates",
                e.getValue()));
    return Map.of(
        "matches",
        matches,
        "matchCount",
        matches.size(),
        "exceptions",
        exceptions,
        "automaticPosting",
        false);
  }
}
