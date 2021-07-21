package io.prometheus.kudu.task;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            Constructor<? extends KuduExporterTask> constructor = Class
                    .forName(configuration.getFetcherClassname())
                    .asSubclass(KuduExporterTask.class)
                    .getConstructor(Integer.class, KuduExporterConfiguration.class, KuduMetricsPool.class);
            ExecutorService threadPool = Executors.newWorkStealingPool();
            while (true) {
                for (int i = configuration.getKuduNodes().size() - 1; i >= 0; i--) {
                    threadPool.submit((Callable<?>) constructor.newInstance(i, this.configuration, this.metricsPool));
                }
                Thread.sleep(configuration.getFetchInterval());
            }
        } catch (ClassNotFoundException e) {
            logger.error(String.format("Fetcher class %s cannot be found.", this.configuration.getFetcherClassname()));
        } catch (InterruptedException e) {
            logger.error(String.format("Fetcher threads running fail (%s).", e.getCause()));
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            logger.error("Fetcher Inner fatal error for invocation target or method change.");
        }
    }


}