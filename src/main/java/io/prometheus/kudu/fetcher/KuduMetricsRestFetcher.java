package io.prometheus.kudu.fetcher;

import com.google.gson.Gson;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.task.KuduExporterTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class KuduMetricsRestFetcher extends KuduExporterTask<List<Map<?, ?>>> {
    private static final String RESPONSE_CHARSET = "UTF-8";

    private URL kuduRestURL;
    private Gson gson;

    public KuduMetricsRestFetcher(
            Integer threadID,
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        super(threadID, configuration, metricsPool);
    }

    private static String getIncludedMetricsStr(List<String> includeMetrics) {
        StringBuilder strBuffer = new StringBuilder();
        for (int i = includeMetrics.size() - 1; i >= 0; i--) {
            strBuffer.append(includeMetrics.get(i).concat(i == 0 ? "" : ","));
        }
        return strBuffer.toString();
    }

    @Override
    protected void start() throws Exception {
        kuduRestURL = new URL(
                String.format("http://%s/metrics?compact=1&metrics=%s",
                        configuration.getFetcherKuduNodes().get(threadID),
                        getIncludedMetricsStr(configuration.getMetricIncludeKeys()))
        );
        gson = new Gson();
    }

    @Override
    protected void process() {
        try (InputStreamReader reader = new InputStreamReader(
                this.kuduRestURL.openConnection().getInputStream(), RESPONSE_CHARSET)) {
            this.metricPool.write(this.threadID, this.gson.fromJson(reader, List.class));
        } catch (UnsupportedEncodingException e) {
            logger.warn("Fetch kudu metric from RestAPI failed for content not decoded with UTF-8.");
        } catch (IOException e) {
            logger.warn("Fetch kudu metric from RestAPI failed for IOException.");
        }
    }

    @Override
    protected void stop() {
    }

}
