package com.platform.service.governance.parser;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态参数解析器
 * 支持系统参数和日期偏移计算
 *
 * 系统参数:
 *   ${current_date}        → yyyyMMdd     (20260818)
 *   ${current_time}         → yyyyMMddHHmmss (20260818143025)
 *   ${current_year}        → yyyy         (2026)
 *   ${current_month}       → yyyyMM       (202608)
 *   ${current_date_dash}   → yyyy-MM-dd   (2026-08-18)
 *   ${current_datetime}    → yyyy-MM-dd HH:mm:ss
 *   ${yesterday}           → 前一天 yyyyMMdd
 *   ${tomorrow}            → 明天 yyyyMMdd
 *
 * 动态日期偏移:
 *   ${yyyyMMdd-1}          → 前一天    (20260817)
 *   ${yyyyMMdd+1}          → 后一天    (20260819)
 *   ${yyyyMMdd-7}          → 前7天
 *   ${yyyy-MM-dd-1}        → 前一天带横杠 (2026-08-17)
 *   ${yyyyMM-1}            → 上月
 *   ${yyyy-1}              → 去年
 */
@Slf4j
@Component
public class ParamParser {

    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private static final Map<String, String> SYSTEM_PATTERNS = new HashMap<>();

    static {
        SYSTEM_PATTERNS.put("current_date", "yyyyMMdd");
        SYSTEM_PATTERNS.put("current_time", "yyyyMMddHHmmss");
        SYSTEM_PATTERNS.put("current_year", "yyyy");
        SYSTEM_PATTERNS.put("current_month", "yyyyMM");
        SYSTEM_PATTERNS.put("current_date_dash", "yyyy-MM-dd");
        SYSTEM_PATTERNS.put("current_datetime", "yyyy-MM-dd HH:mm:ss");
        SYSTEM_PATTERNS.put("yesterday", "yyyyMMdd");
        SYSTEM_PATTERNS.put("tomorrow", "yyyyMMdd");
    }

    public String parse(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Matcher matcher = PARAM_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String expr = matcher.group(1).trim();
            String replacement = resolveExpression(expr);
            matcher.appendReplacement(result, replacement != null ? replacement : matcher.group());
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String resolveExpression(String expr) {
        if (SYSTEM_PATTERNS.containsKey(expr)) {
            return resolveSystemParam(expr);
        }

        if (expr.equalsIgnoreCase("yesterday")) {
            return formatDate(LocalDate.now().minusDays(1), "yyyyMMdd");
        }
        if (expr.equalsIgnoreCase("tomorrow")) {
            return formatDate(LocalDate.now().plusDays(1), "yyyyMMdd");
        }

        return resolveDateOffset(expr);
    }

    private String resolveSystemParam(String paramName) {
        String pattern = SYSTEM_PATTERNS.get(paramName);
        LocalDateTime now = LocalDateTime.now();

        if ("yesterday".equals(paramName)) {
            return formatDate(now.toLocalDate().minusDays(1), pattern);
        }
        if ("tomorrow".equals(paramName)) {
            return formatDate(now.toLocalDate().plusDays(1), pattern);
        }

        return now.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析日期偏移表达式
     * 格式: {datePattern±offset}
     * 例如: yyyyMMdd-1, yyyy-MM-dd+7, yyyyMM-1
     */
    private String resolveDateOffset(String expr) {
        int opIndex = -1;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == '+' || c == '-') {
                if (i > 0 && (Character.isLetter(expr.charAt(i - 1)) || expr.charAt(i - 1) == 'd' || expr.charAt(i - 1) == 'M' || expr.charAt(i - 1) == 'y' || expr.charAt(i - 1) == 'm')) {
                    opIndex = i;
                    break;
                }
            }
        }

        if (opIndex == -1) {
            log.warn("无法解析参数表达式: ${{}}", expr);
            return null;
        }

        String datePattern = expr.substring(0, opIndex);
        String offsetStr = expr.substring(opIndex);

        try {
            int offset = Integer.parseInt(offsetStr);

            if (datePattern.contains("MM") && !datePattern.contains("dd") && !datePattern.contains("DD")) {
                LocalDate target = LocalDate.now().plusMonths(offset);
                return formatDate(target, datePattern);
            } else if (datePattern.contains("yyyy") && !datePattern.contains("MM") && !datePattern.contains("dd")) {
                LocalDate target = LocalDate.now().plusYears(offset);
                return formatDate(target, datePattern);
            } else {
                LocalDate target = LocalDate.now().plusDays(offset);
                return formatDate(target, datePattern);
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析偏移量: ${{}}", expr);
            return null;
        }
    }

    private String formatDate(LocalDate date, String pattern) {
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    public Map<String, String> getAvailableParams() {
        Map<String, String> params = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        params.put("${current_date}", now.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + " (当前日期 yyyyMMdd)");
        params.put("${current_time}", now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + " (当前时间 yyyyMMddHHmmss)");
        params.put("${current_year}", now.format(DateTimeFormatter.ofPattern("yyyy")) + " (当前年份)");
        params.put("${current_month}", now.format(DateTimeFormatter.ofPattern("yyyyMM")) + " (当前月份)");
        params.put("${current_date_dash}", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " (当前日期 yyyy-MM-dd)");
        params.put("${current_datetime}", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " (当前日期时间)");
        params.put("${yesterday}", formatDate(now.toLocalDate().minusDays(1), "yyyyMMdd") + " (昨天)");
        params.put("${tomorrow}", formatDate(now.toLocalDate().plusDays(1), "yyyyMMdd") + " (明天)");
        params.put("${yyyyMMdd-1}", formatDate(now.toLocalDate().minusDays(1), "yyyyMMdd") + " (前一天)");
        params.put("${yyyyMMdd+1}", formatDate(now.toLocalDate().plusDays(1), "yyyyMMdd") + " (后一天)");
        params.put("${yyyyMMdd-7}", formatDate(now.toLocalDate().minusDays(7), "yyyyMMdd") + " (前7天)");
        params.put("${yyyy-MM-dd-1}", formatDate(now.toLocalDate().minusDays(1), "yyyy-MM-dd") + " (前一天带横杠)");
        params.put("${yyyyMM-1}", formatDate(now.toLocalDate().minusMonths(1), "yyyyMM") + " (上月)");
        params.put("${yyyy-1}", formatDate(now.toLocalDate().minusYears(1), "yyyy") + " (去年)");

        return params;
    }
}
