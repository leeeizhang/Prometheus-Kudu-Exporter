package io.prometheus.kudu.reporter;

import io.prometheus.client.Collector;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.task.KuduExporterTask;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class KuduMetricLocalReporter extends KuduExporterTask<Void> {
    private static final Logger logger = LoggerUtils.Logger();

    private Collector kuduMetricCollector;
    private HTTPServer httpServer;

    public KuduMetricLocalReporter(
            Integer threadID,
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        super(threadID, configuration, metricsPool);
    }

    @Override
    protected void start() throws Exception {
        this.kuduMetricCollector = new KuduMetricGeneralCollector(configuration, metricsPool);
        this.httpServer = new HTTPServer(configuration.getLocalReporterPort(), true);
        this.kuduMetricCollector.register();
    }

    @Override
    protected Void process() {
        return null;
    }

    @Override
    protected void stop() {
    }

}