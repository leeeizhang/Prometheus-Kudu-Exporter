package io.prometheus.kudu.reporter;

import io.prometheus.client.Collector;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class KuduMetricReporter extends Collector implements Runnable {
    protected static final Logger logger = LoggerUtils.Logger();

    protected final KuduExporterConfiguration configuration;
    protected final KuduMetricsPool<List<Map<?, ?>>> metricsPool;

    protected KuduMetricReporter(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        this.configuration = configuration;
        this.metricsPool = metricsPool;
    }

    protected abstract Map<String, List<MetricFamilySamples.Sample>> report();

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> metricsResult = new ArrayList<>();
        for (Map.Entry<String, List<MetricFamilySamples.Sample>> entry : report().entrySet()) {
            metricsResult.add(new MetricFamilySamples(
                    entry.getKey(),
                    Type.GAUGE,
                    "",
                    entry.getValue()
            ));
        }
        return metricsResult;
    }

    @Override
    public void run() {
        this.collect();
    }

}