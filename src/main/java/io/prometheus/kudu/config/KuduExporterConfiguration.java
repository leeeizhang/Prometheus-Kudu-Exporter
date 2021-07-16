package io.prometheus.kudu.config;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KuduExporterConfiguration implements Serializable {

    private static final Long FETCH_INTERVAL = 3000L;
    private static final Long PUSH_INTERVAL = 5000L;
    private static final String INCLUDE_KEYWORD = "kudu_";
    private static final String EXCLUDE_KEYWORD = "log";
    private static final Integer REPORTER_PORT = 9055;

    private static KuduExporterConfiguration instance = new KuduExporterConfiguration();

    private final List<URL> kuduMetricURL;
    private Integer reporterPort;
    private Long fetchInterval;
    private Long pushInterval;
    private String[] includeKeyword;
    private String[] excludeKeyword;

    private KuduExporterConfiguration() {
        kuduMetricURL = new ArrayList<>();
        fetchInterval = FETCH_INTERVAL;
        pushInterval = PUSH_INTERVAL;
        reporterPort = REPORTER_PORT;
        includeKeyword = INCLUDE_KEYWORD.split("[;,]");
        excludeKeyword = EXCLUDE_KEYWORD.split("[;,]");
    }

    public static KuduExporterConfiguration getFromConfiguration(Map<String, Object> config)
            throws MalformedURLException {
        if (config != null) {
            Object metricUrls = config.getOrDefault("prom.kudu.metric.url", "");
            Object reporterPort = config.getOrDefault("prom.kudu.metric.reporter-port", REPORTER_PORT);
            Object fetchInterval = config.getOrDefault("prom.kudu.metric.fetch-interval", FETCH_INTERVAL);
            Object pushInterval = config.getOrDefault("prom.kudu.metric.push-interval", PUSH_INTERVAL);
            Object includeKeyword = config.getOrDefault("prom.kudu.metric.include-keyword", INCLUDE_KEYWORD);
            Object excludeKeyword = config.getOrDefault("prom.kudu.metric.exclude-keyword", EXCLUDE_KEYWORD);

            if (metricUrls != null) {
                String[] metricUrlList = metricUrls.toString().split("[;,]");
                for (String url : metricUrlList) {
                    instance.kuduMetricURL.add(new URL(url));
                }
            }
            if (reporterPort != null) {
                instance.reporterPort = Integer.valueOf(reporterPort.toString());
            }
            if (fetchInterval != null) {
                instance.fetchInterval = Long.valueOf(fetchInterval.toString());
            }
            if (pushInterval != null) {
                instance.pushInterval = Long.valueOf(pushInterval.toString());
            }
            if (includeKeyword != null) {
                instance.includeKeyword = includeKeyword.toString().split("[;,]");
            }
            if (excludeKeyword != null) {
                instance.excludeKeyword = excludeKeyword.toString().split("[;,]");
            }
        }
        return instance;
    }

    public static KuduExporterConfiguration getFromConfiguration(String yamlPath)
            throws FileNotFoundException, MalformedURLException {
        return getFromConfiguration(new Yaml().loadAs(new FileInputStream(yamlPath), Map.class));
    }

    public List<URL> getKuduMetricURL() {
        return kuduMetricURL;
    }

    public Long getFetchInterval() {
        return fetchInterval;
    }

    public Long getPushInterval() {
        return pushInterval;
    }

    public Integer getReporterPort() {
        return reporterPort;
    }

    public String[] getIncludeKeyword() {
        return includeKeyword;
    }

    public String[] getExcludeKeyword() {
        return excludeKeyword;
    }
}
