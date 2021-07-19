package io.prometheus.kudu;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.fetcher.KuduMetricFetcherRunner;
import io.prometheus.kudu.reporter.KuduMetricReporterRunner;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.ArgsEntity;
import io.prometheus.kudu.util.LoggerUtils;
import org.kohsuke.args4j.CmdLineParser;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class KuduExporter {

    private static final Logger logger = LoggerUtils.Logger();

    public static void main(String[] args) {
        try {
            // Parser the parameters into ArgsEntity
            ArgsEntity argsEntity = new ArgsEntity() {{
                new CmdLineParser(this).parseArgument(args);
            }};

            // Build configuration and metric pool
            KuduExporterConfiguration configuration = KuduExporterConfiguration.getFromConfiguration(argsEntity);
            KuduMetricsPool<List<Map<?, ?>>> metricsPool = KuduMetricsPool.<List<Map<?, ?>>>build();

            // Start fetcher jobs and the reporter job by custom configuration
            KuduMetricFetcherRunner.run(configuration, metricsPool);
            KuduMetricReporterRunner.run(configuration, metricsPool);
        } catch (Exception e) {
            logger.warning(String.format("Running with Exception: %s", e.getMessage()));
        }
    }

}