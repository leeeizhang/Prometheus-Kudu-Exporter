package io.prometheus.kudu.reporter;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.fetcher.KuduMetricFetcher;
import io.prometheus.kudu.sink.KuduMetricsPool;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
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
            Constructor<? extends KuduMetricReporter> constructor = Class
                    .forName(configuration.getReporterClassname())
                    .asSubclass(KuduMetricReporter.class)
                    .getConstructor(KuduExporterConfiguration.class, KuduMetricsPool.class);
            KuduMetricReporter reporter = constructor.newInstance(this.configuration, this.metricsPool);
            while (true) {
                reporter.run();
                Thread.sleep(configuration.getPushInterval());
            }
        } catch (ClassNotFoundException e) {
            logger.warning("Reporter class not founded.");
        } catch (InterruptedException e) {
            logger.warning("Reporter thread running error.");
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            logger.warning("Reporter Inner fatal error for invocation target or method change.");
        }
    }

}
