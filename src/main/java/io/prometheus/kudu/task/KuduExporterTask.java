package io.prometheus.kudu.task;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class KuduExporterTask<T> implements Callable<T> {
    protected static final Logger logger = LoggerUtils.Logger();

    protected final Integer threadID;
    protected final KuduExporterConfiguration configuration;
    protected final KuduMetricsPool<List<Map<?, ?>>> metricsPool;

    protected KuduExporterTask(
            Integer threadID,
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        this.threadID = threadID;
        this.configuration = configuration;
        this.metricsPool = metricsPool;
    }

    protected abstract void start() throws Exception;

    protected abstract T process() throws Exception;

    protected abstract void stop() throws Exception;

    @Override
    public T call() throws Exception {
        try {
            start();
            return process();
        } finally {
            stop();
        }
    }

}