package io.prometheus.kudu.reporter;

import io.prometheus.client.Collector;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.LoggerUtils;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public abstract class KuduMetricReporter extends Collector {
    protected static final Logger logger = LoggerUtils.Logger();

    protected final KuduExporterConfiguration configuration;
    protected final KuduMetricsPool<List<Map<?, ?>>> metricsPool;

    protected KuduMetricReporter(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        this.configuration = configuration;
        this.metricsPool = metricsPool;
    }

    protected abstract void start() throws Exception;

    @Override
    public abstract List<MetricFamilySamples> collect();

}
