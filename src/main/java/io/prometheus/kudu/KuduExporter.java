package io.prometheus.kudu;

import io.prometheus.client.Collector;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.producer.KuduMetricsFetcherRunner;
import io.prometheus.kudu.reducer.KuduMetricsReporterRunner;
import io.prometheus.kudu.sink.KuduMetricsPool;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class KuduExporter {

    private static final Logger logger = Logger.getLogger(KuduExporter.class.getName());
    private static final String YAML_DEFAULT_PATH = "/home/yzf/kudu-exporter.yaml";

    public static void main(String[] args) {
        try {
            KuduExporterConfiguration configuration = KuduExporterConfiguration
                    .getFromConfiguration(YAML_DEFAULT_PATH);

            HTTPServer reporter = new HTTPServer(configuration.getReporterPort(), true);
            KuduMetricsPool<List<Map<?, ?>>> metricsPool = new KuduMetricsPool<>();

            Thread fetcherRunner = new Thread(KuduMetricsFetcherRunner.builder(configuration, metricsPool));
            fetcherRunner.start();

            Collector reporterRunner = KuduMetricsReporterRunner.builder(configuration, metricsPool);
            reporterRunner.register();
        } catch (Exception e) {
            logger.warning(e.toString());
        }
    }

}