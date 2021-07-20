package io.prometheus.kudu.fetcher;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.LoggerUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class KuduMetricFetcherRunner implements Runnable {
    private static final Logger logger = LoggerUtils.Logger();

    private final KuduExporterConfiguration configuration;
    private final KuduMetricsPool<List<Map<?, ?>>> metricsPool;

    private KuduMetricFetcherRunner(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        this.configuration = configuration;
        this.metricsPool = metricsPool;
    }

    public static void run(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        Thread thread = new Thread(new KuduMetricFetcherRunner(configuration, metricsPool));
        thread.start();
    }

    @Override
    public void run() {
        try {
            Constructor<? extends KuduMetricFetcher> constructor = Class
                    .forName(configuration.getFetcherClassname())
                    .asSubclass(KuduMetricFetcher.class)
                    .getConstructor(String.class, Long.class, KuduExporterConfiguration.class);
            ExecutorService threadPool = Executors.newWorkStealingPool();
            while (true) {
                for (int i = configuration.getKuduNodes().size() - 1; i >= 0; i--) {
                    this.metricsPool.put(i, threadPool.submit(
                            constructor.newInstance(
                                    this.configuration.getKuduNodes().get(i),
                                    this.configuration.getFetchInterval(),
                                    this.configuration
                            )
                    ));
                }
                Thread.sleep(configuration.getFetchInterval());
            }
        } catch (ClassNotFoundException e) {
            logger.warning("Fetcher class not founded.");
        } catch (InterruptedException e) {
            logger.warning("Fetcher thread running error.");
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            logger.warning("Fetcher Inner fatal error for invocation target or method change.");
        }
    }

}
