package io.prometheus.kudu.reporter;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class KuduMetricReporterRunner implements Runnable {
    private static final Logger logger = Logger.getLogger(KuduMetricReporterRunner.class.getName());

    private final KuduExporterConfiguration configuration;
    private final KuduMetricsPool<List<Map<?, ?>>> metricsPool;

    public KuduMetricReporterRunner(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        this.configuration = configuration;
        this.metricsPool = metricsPool;
    }

    public static void run(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        Thread thread = new Thread(new KuduMetricReporterRunner(configuration, metricsPool));
        thread.start();
    }

    @Override
    public void run() {
        try {
            ExecutorService threadPool = Executors.newWorkStealingPool();
            Class reporterClass = Class.forName(configuration.getReporterClassname());
            Class[] parameterTypes = {KuduExporterConfiguration.class, KuduMetricsPool.class};
            Constructor<KuduMetricReporter> constructor = reporterClass.getConstructor(parameterTypes);
            KuduMetricReporter reporter = constructor.newInstance(this.configuration, this.metricsPool);
            while (true) {
                reporter.run();
                Thread.sleep(configuration.getPushInterval());
            }
        } catch (Exception e) {
            logger.warning("Reporter Runner Meet Some Issues.");
        }
    }

}
