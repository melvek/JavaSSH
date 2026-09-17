package com.mestrap.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableReplacer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final int MAX_DEPTH = 4;

    /**
     * 替换变量。若替换后仍存在未解析的 ${xxx}，抛出 JsshException。
     */
    public static String replace(String template, Map<String, Object> vars) {
        if (template == null || template.isEmpty()) return template;

        String current = template;
        for (int i = 0; i < MAX_DEPTH; i++) {
            String next = replaceOnce(current, vars);
            if (next.equals(current)) {
                break;
            }
            current = next;
        }

        // 检查是否还有未解析的占位符
        List<String> missing = findMissingKeys(current);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Unresolved variable(s): " + missing + " in \"" + template + "\"");
        }

        return current;
    }

    private static String replaceOnce(String template, Map<String, Object> vars) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1).trim();
            String value = vars.containsKey(key)
                    ? String.valueOf(vars.get(key))
                    : m.group(0);
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static List<String> findMissingKeys(String s) {
        List<String> keys = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(s);
        while (m.find()) {
            keys.add(m.group(1).trim());
        }
        return keys;
    }
}