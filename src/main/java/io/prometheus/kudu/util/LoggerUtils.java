package io.prometheus.kudu.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class LoggerUtils {

    public static Logger Logger() {
        return LogManager.getRootLogger();
    }

}
