package io.prometheus.kudu.fetcher;

import com.google.gson.Gson;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.task.KuduExporterTask;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KuduMetricsRestFetcher extends KuduExporterTask<List<Map<?, ?>>> {
    private static final Logger logger = LoggerUtils.Logger();

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
                        configuration.getKuduNodes().get(threadID),
                        getIncludedMetricsStr(configuration.getIncludeKeyword()))
        );
        gson = new Gson();
    }

    @Override
    protected List<Map<?, ?>> process() {
        try (InputStreamReader reader = new InputStreamReader(
                this.kuduRestURL.openConnection().getInputStream(), RESPONSE_CHARSET)) {
            List<Map<?, ?>> ans = this.gson.fromJson(reader, List.class);
            this.metricsPool.write(this.threadID, ans);
            return ans;
        } catch (UnsupportedEncodingException e) {
            logger.warn("Fetch kudu metric from RestAPI failed for content not decoded with UTF-8.");
        } catch (IOException e) {
            logger.warn("Fetch kudu metric from RestAPI failed for IOException.");
        }
        return Collections.singletonList(new HashMap<>(16));
    }

    @Override
    protected void stop() {
    }

}
