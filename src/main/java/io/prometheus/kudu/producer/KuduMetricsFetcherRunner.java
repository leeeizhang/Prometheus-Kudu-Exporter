package io.prometheus.kudu.producer;

import com.google.gson.Gson;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class KuduMetricsFetcherRunner implements Runnable {

    private static final Logger logger = Logger.getLogger(KuduMetricsFetcherRunner.class.getName());

    private final KuduExporterConfiguration configuration;
    private final KuduMetricsPool<List<Map<?, ?>>> metricsPool;
    private final Gson gson;

    private KuduMetricsFetcherRunner(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        this.configuration = configuration;
        this.metricsPool = metricsPool;
        this.gson = new Gson();
    }

    public static KuduMetricsFetcherRunner builder(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        return new KuduMetricsFetcherRunner(configuration, metricsPool);
    }

    @Override
    public void run() {
        ExecutorService threadPool = Executors.newWorkStealingPool();
        while (true) {
            for (int i = configuration.getKuduMetricURL().size() - 1; i >= 0; i--) {
                this.metricsPool.put(i, threadPool.submit(
                        new KuduMetricsFetcher(configuration.getKuduMetricURL().get(i), gson)));
            }
            try {
                Thread.sleep(configuration.getFetchInterval());
            } catch (InterruptedException e) {
                logger.warning(e.toString());
            }
        }
    }

}
