package io.prometheus.kudu.util;

public class StringUtils {

    public static boolean isEmpty(Object obj) {
        return obj == null || isEmpty(obj.toString());
    }

    public static boolean isEmpty(String str) {
        return str.isEmpty();
    }
}
