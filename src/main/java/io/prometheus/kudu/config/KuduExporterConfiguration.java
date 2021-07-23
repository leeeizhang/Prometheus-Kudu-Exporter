/*
 * Copyright RyanCheung98@163.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.kudu.config;

import io.prometheus.kudu.fetcher.KuduMetricsRestFetcher;
import io.prometheus.kudu.reporter.KuduMetricLocalReporter;
import io.prometheus.kudu.util.ArgsEntity;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.*;

/**
 * Loading and lasting the configuration of exporter.
 */
public class KuduExporterConfiguration implements Serializable {
    protected static final Logger logger = LoggerUtils.Logger();

    /**
     * Keywords to be using in properties
     */
    public static final String METRIC_INCLUDE_KEY = "prom.kudu.metric.include";
    public static final String METRIC_EXCLUDE_KEY = "prom.kudu.metric.exclude";

    public static final String FETCHER_CLASSNAME_KEY = "prom.kudu.fetcher.classname";
    public static final String FETCHER_KUDU_NODES_KEY = "prom.kudu.fetcher.kudu-nodes";
    public static final String FETCHER_INTERVAL_KEY = "prom.kudu.fetcher.interval";

    public static final String REPORTER_CLASSNAME_KEY = "prom.kudu.reporter.classname";
    public static final String REPORTER_LOCAL_PORT_KEY = "prom.kudu.reporter.local.port";
    public static final String REPORTER_PUSH_GATEWAY_HOST_KEY = "prom.kudu.reporter.pushgateway.host";
    public static final String REPORTER_PUSH_GATEWAY_INTERVAL_KEY = "prom.kudu.reporter.pushgateway.interval";

    /**
     * Default value to fill the properties
     */
    public static final List<String> METRIC_INCLUDE_DEFAULT = Collections.singletonList("");
    public static final List<String> METRIC_EXCLUDE_DEFAULT = Collections.singletonList("");

    public static final String FETCHER_CLASSNAME_DEFAULT = KuduMetricsRestFetcher.class.getName();
    public static final List<String> FETCHER_KUDU_NODES_DEFAULT = Collections.singletonList("127.0.0.1:8050");
    public static final Integer FETCHER_INTERVAL_DEFAULT = 10000;

    public static final String REPORTER_CLASSNAME_DEFAULT = KuduMetricLocalReporter.class.getName();
    public static final Integer REPORTER_LOCAL_PORT_DEFAULT = 9055;
    public static final String REPORTER_PUSH_GATEWAY_HOST_DEFAULT = "127.0.0.1:9091";
    public static final Integer REPORTER_PUSH_GATEWAY_INTERVAL_DEFAULT = 10000;


    protected final Properties properties;

    /**
     * Private Constructor to init the properties
     */
    protected KuduExporterConfiguration() {
        this.properties = new Properties() {{
            this.put(METRIC_INCLUDE_KEY, METRIC_INCLUDE_DEFAULT);
            this.put(METRIC_EXCLUDE_KEY, METRIC_EXCLUDE_DEFAULT);
            this.put(FETCHER_CLASSNAME_KEY, FETCHER_CLASSNAME_DEFAULT);
            this.put(FETCHER_KUDU_NODES_KEY, FETCHER_KUDU_NODES_DEFAULT);
            this.put(FETCHER_INTERVAL_KEY, FETCHER_INTERVAL_DEFAULT);
            this.put(REPORTER_CLASSNAME_KEY, REPORTER_CLASSNAME_DEFAULT);
            this.put(REPORTER_LOCAL_PORT_KEY, REPORTER_LOCAL_PORT_DEFAULT);
            this.put(REPORTER_PUSH_GATEWAY_HOST_KEY, REPORTER_PUSH_GATEWAY_HOST_DEFAULT);
            this.put(REPORTER_PUSH_GATEWAY_INTERVAL_KEY, REPORTER_PUSH_GATEWAY_INTERVAL_DEFAULT);
        }};
    }

    /**
     * Set the key and value in properties
     *
     * @param key   the keyword in properties
     * @param value value in properties
     */
    public <T> void setValue(String key, T value) {
        this.properties.put(key, value);
    }

    /**
     * Get the value by keyword in properties
     *
     * @param key          the keyword in properties, and it should be static announced class
     * @param defaultValue if properties do not exist keyword, then return default value
     * @return the value in properties, if not exists then default value as param given
     */
    public <T> T getValue(String key, T defaultValue) {
        return (T) this.properties.getOrDefault(key, defaultValue);
    }

    /**
     * Get configuration from args input
     *
     * @param argsEntity args
     * @return Instance
     */
    public static KuduExporterConfiguration getConfiguration(ArgsEntity argsEntity) {
        return getConfiguration(
                argsEntity.getConfigPath(),
                argsEntity.getIncludePath(),
                argsEntity.getExcludePath()
        );
    }

    /**
     * Get configuration from file
     *
     * @param confPath    the configuration file 'kudu-exporter.yml' path
     * @param includePath the include-metrics path
     * @param excludePath the exclude-metrics path
     * @return instance
     */
    public static KuduExporterConfiguration getConfiguration(
            String confPath,
            String includePath,
            String excludePath) {
        Map<String, Object> config = null;
        List<String> includeMetrics = null, excludeMetrics = null;
        try {
            config = new Yaml().loadAs(new FileInputStream(confPath), Map.class);
            includeMetrics = new ArrayList<String>(64) {{
                try (Scanner sc = new Scanner(new File(includePath))) {
                    while (sc.hasNext()) {
                        this.add(sc.next());
                    }
                }
            }};
            excludeMetrics = new ArrayList<String>(8) {{
                try (Scanner sc = new Scanner(new File(excludePath))) {
                    while (sc.hasNext()) {
                        this.add(sc.next());
                    }
                }
            }};
        } catch (FileNotFoundException e) {
            logger.warn("configuration file not found, sysytem will be configure as default.", e);
        }
        return getConfiguration(config, includeMetrics, excludeMetrics);
    }

    /**
     * Get configuration
     *
     * @param config         configuration details as a Map
     * @param includeMetrics include metric keywords list
     * @param excludeMetrics exclude metric keywords list
     * @return instance
     */
    public static KuduExporterConfiguration getConfiguration(
            Map<String, Object> config,
            List<String> includeMetrics,
            List<String> excludeMetrics) {
        KuduExporterConfiguration configuration = new KuduExporterConfiguration();
        if (config != null && !config.isEmpty()) {
            for (Map.Entry<String, Object> conf : config.entrySet()) {
                if (conf.getValue() instanceof String) {
                    configuration.setValue(conf.getKey(), conf.getValue().toString());
                } else if (conf.getValue() instanceof Long) {
                    configuration.setValue(conf.getKey(), Long.valueOf(conf.getValue().toString()));
                } else if (conf.getValue() instanceof Integer || conf.getValue() instanceof Short
                        || conf.getValue() instanceof Byte) {
                    configuration.setValue(conf.getKey(), Integer.valueOf(conf.getValue().toString()));
                } else if (conf.getValue() instanceof Double || conf.getValue() instanceof Float) {
                    configuration.setValue(conf.getKey(), Double.valueOf(conf.getValue().toString()));
                } else if (conf.getValue() instanceof Collection) {
                    configuration.setValue(conf.getKey(), conf.getValue());
                }
            }
        }
        if (includeMetrics != null && !includeMetrics.isEmpty()) {
            configuration.setValue(METRIC_INCLUDE_KEY, includeMetrics);
        }
        if (excludeMetrics != null && !excludeMetrics.isEmpty()) {
            configuration.setValue(METRIC_EXCLUDE_KEY, excludeMetrics);
        }
        return configuration;
    }

    public List<String> getMetricIncludeKeys() {
        return this.getValue(METRIC_INCLUDE_KEY, METRIC_INCLUDE_DEFAULT);
    }

    public List<String> getMetricExcludeKeys() {
        return this.getValue(METRIC_EXCLUDE_KEY, METRIC_EXCLUDE_DEFAULT);
    }

    public String getFetcherClassName() {
        return this.getValue(FETCHER_CLASSNAME_KEY, FETCHER_CLASSNAME_DEFAULT);
    }

    public List<String> getFetcherKuduNodes() {
        return this.getValue(FETCHER_KUDU_NODES_KEY, FETCHER_KUDU_NODES_DEFAULT);
    }

    public Integer getFetcherInterval() {
        return this.getValue(FETCHER_INTERVAL_KEY, FETCHER_INTERVAL_DEFAULT);
    }

    public String getReporterClassName() {
        return this.getValue(REPORTER_CLASSNAME_KEY, REPORTER_CLASSNAME_DEFAULT);
    }

    public Integer getLocalReporterPort() {
        return this.getValue(REPORTER_LOCAL_PORT_KEY, REPORTER_LOCAL_PORT_DEFAULT);
    }

    public String getPushGatewayReporterHost() {
        return this.getValue(REPORTER_PUSH_GATEWAY_HOST_KEY, REPORTER_PUSH_GATEWAY_HOST_DEFAULT);
    }

    public Integer getPushGatewayReporterInterval() {
        return this.getValue(REPORTER_PUSH_GATEWAY_INTERVAL_KEY, REPORTER_PUSH_GATEWAY_INTERVAL_DEFAULT);
    }

}
