package com.mestrap.utils;

import java.util.Locale;

/**
 * 布尔值判断工具。
 *
 * @author melvek
 */
public final class BoolUtil {

    private BoolUtil() {}

    /**
     * 判断值是否为"真"。
     * 支持 Boolean.TRUE，以及字符串 "true" / "1" / "yes"（忽略大小写）。
     *
     * @param v 待判断的值
     * @return true 表示真值；null、false、其他字符串均返回 false
     */
    public static boolean isTruthy(Object v) {
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean) {
            return (Boolean) v;
        }

        String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }
}