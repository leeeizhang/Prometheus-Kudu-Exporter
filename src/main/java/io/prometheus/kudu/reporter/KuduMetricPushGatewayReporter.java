package io.prometheus.kudu.reporter;

import io.prometheus.client.Collector;
import io.prometheus.client.exporter.PushGateway;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.task.KuduExporterTask;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class KuduMetricPushGatewayReporter extends KuduExporterTask<Void> {
    private static final Logger logger = LoggerUtils.Logger();

    private Collector kuduMetricCollector;
    private PushGateway pushGateway;

    public KuduMetricPushGatewayReporter(
            Integer threadID,
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        super(threadID, configuration, metricsPool);
    }

    @Override
    protected void start() throws Exception {
        this.kuduMetricCollector = new KuduMetricGeneralCollector(configuration, metricsPool);
        this.pushGateway = new PushGateway(configuration.getPushgatewayURL());
        this.pushGateway.push(kuduMetricCollector, "kudu");
    }

    @Override
    protected Void process() throws Exception {
        this.pushGateway.push(kuduMetricCollector, "kudu");
        return null;
    }

    @Override
    protected void stop() {

    }
}
