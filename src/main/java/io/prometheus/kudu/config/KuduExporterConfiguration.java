package io.prometheus.kudu.config;

import io.prometheus.kudu.util.ArgsEntity;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.*;

public class KuduExporterConfiguration implements Serializable {
    private static final Logger logger = LoggerUtils.Logger();

    private static final String FETCHER_CLASSNAME = "io.prometheus.kudu.fetcher.KuduMetricsLocalReporter";
    private static final String REPORTER_CLASSNAME = "io.prometheus.kudu.reporter.KuduMetricsLocalReporter";
    private static final String PUSH_GATEWAY_URL = "http://127.0.0.1:9091/metrics";
    private static final Integer LOCAL_REPORTER_PORT = 9055;
    private static final Long FETCH_INTERVAL = 3000L;
    private static final Long PUSH_INTERVAL = 5000L;

    private String fetcherClassname;
    private String reporterClassname;

    private List<String> kuduNodes;
    private Long fetchInterval;

    private String pushgatewayURL;
    private Integer localReporterPort;
    private Long pushInterval;

    private List<String> includeKeyword;
    private List<String> excludeKeyword;

    public static KuduExporterConfiguration getFromConfiguration(
            ArgsEntity argsEntity) throws FileNotFoundException {
        return getFromConfiguration(
                argsEntity.getConfigPath(),
                argsEntity.getIncludePath(),
                argsEntity.getExcludePath()
        );
    }

    public static KuduExporterConfiguration getFromConfiguration(
            String yamlPath,
            String includePath,
            String excludePath) throws FileNotFoundException {
        Map<String, Object> config = new Yaml().loadAs(new FileInputStream(yamlPath), Map.class);
        List<String> includeMetrics = new ArrayList<String>(64) {{
            try (Scanner sc = new Scanner(new File(includePath))) {
                while (sc.hasNext()) {
                    this.add(sc.next());
                }
            }
        }};
        List<String> excludeMetrics = new ArrayList<String>(8) {{
            try (Scanner sc = new Scanner(new File(excludePath))) {
                while (sc.hasNext()) {
                    this.add(sc.next());
                }
            }
        }};
        return new KuduExporterConfiguration().reload(config, includeMetrics, excludeMetrics);
    }

    private KuduExporterConfiguration() {
        fetcherClassname = FETCHER_CLASSNAME;
        reporterClassname = REPORTER_CLASSNAME;

        kuduNodes = new ArrayList<>(8);
        fetchInterval = FETCH_INTERVAL;

        pushgatewayURL = PUSH_GATEWAY_URL;
        localReporterPort = LOCAL_REPORTER_PORT;
        pushInterval = PUSH_INTERVAL;

        includeKeyword = new ArrayList<>(64);
        excludeKeyword = new ArrayList<>(8);
    }

    protected KuduExporterConfiguration reload(
            Map<String, Object> config,
            List<String> includeMetrics,
            List<String> excludeMetrics) {
        if (config != null && !config.isEmpty()) {
            try {
                Class.forName(
                        this.fetcherClassname = config.getOrDefault(
                                "prom.kudu.metric.fetcher-classname",
                                this.fetcherClassname).toString()
                );
            } catch (ClassNotFoundException e) {
                logger.error("'fetcher-classname' cannot been found in 'kudu-exporter.yml'.");
            }

            try {
                Class.forName(
                        this.reporterClassname = config.getOrDefault(
                                "prom.kudu.metric.reporter-classname",
                                this.reporterClassname).toString()
                );
            } catch (ClassNotFoundException e) {
                logger.error("'reporter-classname' cannot been found in 'kudu-exporter.yml'.");
            }

            try {
                this.kuduNodes = (List<String>) config.getOrDefault(
                        "prom.kudu.metric.kudu-nodes",
                        this.kuduNodes
                );
            } catch (Exception e) {
                logger.error("'kudu-nodes' format mismatch in 'kudu-exporter.yml'.");
            }

            try {
                this.fetchInterval = Long.valueOf(config.getOrDefault(
                        "prom.kudu.metric.fetch-interval",
                        this.fetchInterval).toString());
            } catch (Exception e) {
                logger.error("'fetch-interval' is not long value in 'kudu-exporter.yml'.");
            }

            try {
                this.pushgatewayURL = config.getOrDefault(
                        "prom.kudu.metric.pushgateway",
                        this.pushgatewayURL).toString();
            } catch (Exception e) {
                logger.error("'pushgateway' value mismatched in 'kudu-exporter.yml'.");
            }

            try {
                this.localReporterPort = Integer.valueOf(config.getOrDefault(
                        "prom.kudu.metric.reporter-port",
                        this.localReporterPort).toString());
            } catch (Exception e) {
                logger.error("'reporter-port' is not integer value in 'kudu-exporter.yml'.");
            }

            try {
                this.pushInterval = Long.valueOf(config.getOrDefault(
                        "prom.kudu.metric.push-interval",
                        this.pushInterval).toString());
            } catch (Exception e) {
                logger.error("'push-interval' is not long value in 'kudu-exporter.yml'.");
            }
        }

        if (includeMetrics != null && !includeMetrics.isEmpty()) {
            try {
                this.includeKeyword = includeMetrics;
            } catch (Exception e) {
                logger.error("'include-metrics' cannot be parsed.");
            }
        }

        if (excludeMetrics != null && !excludeMetrics.isEmpty()) {
            try {
                this.excludeKeyword = excludeMetrics;
            } catch (Exception e) {
                logger.error("'exclude-metrics' cannot be parsed.");
            }
        }

        return this;
    }

    public String getFetcherClassname() {
        return fetcherClassname;
    }

    public void setFetcherClassname(String fetcherClassname) {
        this.fetcherClassname = fetcherClassname;
    }

    public String getReporterClassname() {
        return reporterClassname;
    }

    public void setReporterClassname(String reporterClassname) {
        this.reporterClassname = reporterClassname;
    }

    public List<String> getKuduNodes() {
        return kuduNodes;
    }

    public void setKuduNodes(List<String> kuduNodes) {
        this.kuduNodes = kuduNodes;
    }

    public Long getFetchInterval() {
        return fetchInterval;
    }

    public void setFetchInterval(Long fetchInterval) {
        this.fetchInterval = fetchInterval;
    }

    public String getPushgatewayURL() {
        return pushgatewayURL;
    }

    public void setPushgatewayURL(String pushgatewayURL) {
        this.pushgatewayURL = pushgatewayURL;
    }

    public Integer getLocalReporterPort() {
        return localReporterPort;
    }

    public void setLocalReporterPort(Integer localReporterPort) {
        this.localReporterPort = localReporterPort;
    }

    public Long getPushInterval() {
        return pushInterval;
    }

    public void setPushInterval(Long pushInterval) {
        this.pushInterval = pushInterval;
    }

    public List<String> getIncludeKeyword() {
        return includeKeyword;
    }

    public void setIncludeKeyword(List<String> includeKeyword) {
        this.includeKeyword = includeKeyword;
    }

    public List<String> getExcludeKeyword() {
        return excludeKeyword;
    }

    public void setExcludeKeyword(List<String> excludeKeyword) {
        this.excludeKeyword = excludeKeyword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KuduExporterConfiguration that = (KuduExporterConfiguration) o;
        return Objects.equals(fetcherClassname, that.fetcherClassname) && Objects.equals(reporterClassname, that.reporterClassname) && Objects.equals(kuduNodes, that.kuduNodes) && Objects.equals(fetchInterval, that.fetchInterval) && Objects.equals(pushgatewayURL, that.pushgatewayURL) && Objects.equals(localReporterPort, that.localReporterPort) && Objects.equals(pushInterval, that.pushInterval) && Objects.equals(includeKeyword, that.includeKeyword) && Objects.equals(excludeKeyword, that.excludeKeyword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fetcherClassname, reporterClassname, kuduNodes, fetchInterval, pushgatewayURL, localReporterPort, pushInterval, includeKeyword, excludeKeyword);
    }

    @Override
    public String toString() {
        return "KuduExporterConfiguration{" +
                "fetcherClassname='" + fetcherClassname + '\'' +
                ", reporterClassname='" + reporterClassname + '\'' +
                ", kuduNodes=" + kuduNodes +
                ", fetchInterval=" + fetchInterval +
                ", pushgatewayURL='" + pushgatewayURL + '\'' +
                ", localReporterPort=" + localReporterPort +
                ", pushInterval=" + pushInterval +
                ", includeKeyword=" + includeKeyword +
                ", excludeKeyword=" + excludeKeyword +
                '}';
    }
}
