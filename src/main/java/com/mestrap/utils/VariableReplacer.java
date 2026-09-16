package com.mestrap.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableReplacer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final int MAX_DEPTH = 4;

    /**
     * 递归替换变量，最多 4 层。
     * 例如：
     *   vars: { a: "${b}", b: "${c}", c: "hello" }
     *   replace("${a}") → "${b}" → "${c}" → "hello"
     */
    public static String replace(String template, Map<String, Object> vars) {
        if (template == null || template.isEmpty()) return template;

        String current = template;
        for (int i = 0; i < MAX_DEPTH; i++) {
            String next = replaceOnce(current, vars);
            if (next.equals(current)) {
                // 不再变化，提前结束
                return next;
            }
            current = next;
        }
        return current;
    }

    /**
     * 单次替换：把当前字符串里所有 ${key} 用 vars 里的值替换掉。
     * 找不到的 key 保留原样（不替换）。
     */
    private static String replaceOnce(String template, Map<String, Object> vars) {
        if (template == null || template.isEmpty()) return template;

        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1).trim();
            String value = vars.containsKey(key)
                    ? String.valueOf(vars.get(key))
                    : m.group(0);   // 未命中保留 ${key}
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}