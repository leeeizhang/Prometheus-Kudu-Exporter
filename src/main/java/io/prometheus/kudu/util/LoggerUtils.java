package io.prometheus.kudu.util;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import io.prometheus.kudu.KuduExporter;


public class LoggerUtils {

    public static Logger Logger() {
        return LoggerFactory.getLogger(KuduExporter.class);
    }

}
