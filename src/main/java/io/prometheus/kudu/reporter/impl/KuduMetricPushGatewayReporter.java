package io.prometheus.kudu.reporter.impl;

import io.prometheus.client.exporter.PushGateway;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.LoggerUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class KuduMetricPushGatewayReporter extends KuduMetricLocalReporter {
    private static final Logger logger = LoggerUtils.Logger();

    private final PushGateway pushGateway;

    public KuduMetricPushGatewayReporter(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) throws Exception {
        super(configuration, metricsPool);
        this.pushGateway = new PushGateway(configuration.getPushgatewayURL());//TODO: PushGateway Authentication Support
    }

    @Override
    public void run() {
        try {
            this.pushGateway.push(this, "kudu");
        } catch (IOException e) {
            logger.warning("Fail to push metrics to prometheus-pushgateway.");
        }
    }

    @Override
    public Map<String, List<MetricFamilySamples.Sample>> report() {
        return super.report();
    }
}
