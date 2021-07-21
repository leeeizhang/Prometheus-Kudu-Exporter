package io.prometheus.kudu.fetcher.impl;

import com.google.gson.Gson;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.fetcher.KuduMetricFetcher;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;

public class KuduMetricsRestFetcher extends KuduMetricFetcher {
    private static final Logger logger = LoggerUtils.Logger();

    private static final String RESPONSE_CHARSET = "UTF-8";

    private final URL kuduRestURL;
    private final Gson gson;

    private static String getIncludedMetricsStr(KuduExporterConfiguration configuration) {
        StringBuilder strBuffer = new StringBuilder();
        for (int i = configuration.getIncludeKeyword().size() - 1; i >= 0; i--) {
            strBuffer.append(configuration.getIncludeKeyword().get(i).concat(i == 0 ? "" : ","));
        }
        return strBuffer.toString();
    }

    public KuduMetricsRestFetcher(
            String kuduNode,
            Long fetchInterval,
            KuduExporterConfiguration configuration) throws Exception {
        super(kuduNode, fetchInterval, configuration);
        kuduRestURL = new URL(String.format("http://%s/metrics?compact=1&metrics=%s",
                kuduNode, getIncludedMetricsStr(configuration)));
        gson = new Gson();
    }

    @Override
    protected List<Map<?, ?>> fetch() {
        try (InputStreamReader reader = new InputStreamReader(
                this.kuduRestURL.openConnection().getInputStream(), RESPONSE_CHARSET)) {
            return this.gson.fromJson(reader, List.class);
        } catch (UnsupportedEncodingException e) {
            logger.warn("Fetch kudu metric from RestAPI failed for content not decoded with UTF-8.");
        } catch (IOException e) {
            logger.warn("Fetch kudu metric from RestAPI failed for IOException.");
        }
        return Collections.singletonList(new HashMap<>(16));
    }

}
