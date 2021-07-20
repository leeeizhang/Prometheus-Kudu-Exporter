package io.prometheus.kudu.fetcher.impl;

import com.google.gson.Gson;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.fetcher.KuduMetricFetcher;
import io.prometheus.kudu.util.LoggerUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

public class KuduMetricsRestFetcher extends KuduMetricFetcher {
    private static final Logger logger = LoggerUtils.Logger();

    private static final String RESPONSE_CHARSET = "UTF-8";

    private final URL kuduRestURL;
    private final Gson gson;

    public KuduMetricsRestFetcher(
            String kuduNode,
            Long fetchInterval,
            KuduExporterConfiguration configuration) throws Exception {
        super(kuduNode, fetchInterval, configuration);
        kuduRestURL = new URL(String.format("http://%s/metrics", kuduNode));
        gson = new Gson();
    }

    @Override
    protected List<Map<?, ?>> fetch() {
        try (InputStreamReader reader = new InputStreamReader(
                this.kuduRestURL.openConnection().getInputStream(), RESPONSE_CHARSET)) {
            return this.gson.fromJson(reader, List.class);
        } catch (UnsupportedEncodingException e) {
            logger.warning("Fetch Kudu Metric Rest API Failed for Content Not Encoded with UTF-8.");
        } catch (IOException e) {
            logger.warning("Fetch Kudu Metric Rest API Failed for IOException.");
        }
        return Collections.singletonList(new HashMap<>());
    }

}
