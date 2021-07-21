package io.prometheus.kudu.fetcher;

import com.sun.org.slf4j.internal.Logger;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.util.LoggerUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class KuduMetricFetcher implements Callable<List<Map<?, ?>>> {
    protected static final Logger logger = LoggerUtils.Logger();

    protected final String kuduNode;
    protected final Long fetchInterval;
    protected final KuduExporterConfiguration configuration;

    protected KuduMetricFetcher(
            String kuduNode,
            Long fetchInterval,
            KuduExporterConfiguration configuration) {
        this.kuduNode = kuduNode;
        this.fetchInterval = fetchInterval;
        this.configuration = configuration;
    }

    protected abstract List<Map<?, ?>> fetch() throws Exception;

    @Override
    public List<Map<?, ?>> call() throws Exception {
        return this.fetch();
    }

}