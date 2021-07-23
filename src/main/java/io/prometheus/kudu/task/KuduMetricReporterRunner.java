package io.prometheus.kudu.task;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class KuduMetricReporterRunner implements Runnable {
    private static final Logger logger = LoggerUtils.Logger();

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
            Constructor<? extends KuduExporterTask> constructor = Class
                    .forName(configuration.getReporterClassName())
                    .asSubclass(KuduExporterTask.class)
                    .getConstructor(Integer.class, KuduExporterConfiguration.class, KuduMetricsPool.class);
            KuduExporterTask reporter = constructor.newInstance(0, this.configuration, this.metricsPool);
            reporter.start();
            while (true) {
                reporter.process();
                Thread.sleep(configuration.getPushGatewayReporterInterval());
            }
        } catch (ClassNotFoundException e) {
            logger.error(String.format("Reporter class %s cannot be found.", this.configuration.getReporterClassName()));
        } catch (InterruptedException e) {
            logger.error(String.format("Reporter threads running fail (%s).", e.getCause()));
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            logger.error("Reporter Inner fatal error for invocation target or method change.");
        } catch (Exception e) {
        }
    }

}
