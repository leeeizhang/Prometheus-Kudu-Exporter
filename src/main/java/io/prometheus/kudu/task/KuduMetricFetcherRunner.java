package io.prometheus.kudu.task;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricPool;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KuduMetricFetcherRunner implements Runnable {
    private static final Logger logger = LoggerUtils.Logger();

    private final KuduExporterConfiguration configuration;
    private final KuduMetricPool<List<Map<?, ?>>> metricsPool;

    private KuduMetricFetcherRunner(
            KuduExporterConfiguration configuration,
            KuduMetricPool<List<Map<?, ?>>> metricsPool) {
        this.configuration = configuration;
        this.metricsPool = metricsPool;
    }

    public static void run(
            KuduExporterConfiguration configuration,
            KuduMetricPool<List<Map<?, ?>>> metricsPool) {
        Thread thread = new Thread(new KuduMetricFetcherRunner(configuration, metricsPool));
        thread.start();
    }

    @Override
    public void run() {
        try {
            Constructor<? extends KuduExporterTask> constructor = Class
                    .forName(configuration.getFetcherClassName())
                    .asSubclass(KuduExporterTask.class)
                    .getConstructor(Integer.class, KuduExporterConfiguration.class, KuduMetricPool.class);
            ExecutorService threadPool = Executors.newWorkStealingPool();
            while (true) {
                for (int i = configuration.getFetcherKuduNodes().size() - 1; i >= 0; i--) {
                    threadPool.submit(constructor.newInstance(i, this.configuration, this.metricsPool));
                }
                Thread.sleep(configuration.getFetcherInterval());
            }
        } catch (ClassNotFoundException e) {
            logger.error(String.format("Fetcher class %s cannot be found.", this.configuration.getFetcherClassName()));
        } catch (InterruptedException e) {
            logger.error(String.format("Fetcher threads running fail (%s).", e.getCause()));
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            logger.error("Fetcher Inner fatal error for invocation target or method change.");
        }
    }


}
