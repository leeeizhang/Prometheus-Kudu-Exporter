package io.prometheus.kudu.util;

public class StringUtils {

    public static boolean isEmpty(Object obj) {
        return obj == null || isEmpty(obj.toString());
    }

    public static boolean isEmpty(String str) {
        return str.isEmpty();
    }

    public static String getOrDefault(String str, String defaultStr) {
        return getOrDefault(str, defaultStr);
    }

    public static String getOrDefault(Object str, Object defaultStr) {
        return isEmpty(str) ? defaultStr.toString() : str.toString();
    }

}
