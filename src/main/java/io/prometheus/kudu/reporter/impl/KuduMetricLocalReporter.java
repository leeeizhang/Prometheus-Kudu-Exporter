package io.prometheus.kudu.reporter.impl;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.reporter.KuduMetricReporter;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.LoggerUtils;
import io.prometheus.kudu.util.MetricSampleTemplate;
import io.prometheus.kudu.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class KuduMetricLocalReporter extends KuduMetricReporter {
    private static final Logger logger = LoggerUtils.Logger();

    private static final String SAMPLE_PREFIX = "kudu_";

    private final HTTPServer server;
    private final MetricSampleTemplate template;

    public KuduMetricLocalReporter(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) throws Exception {
        super(configuration, metricsPool);
        this.server = new HTTPServer(configuration.getLocalReporterPort(), true);
        this.template = MetricSampleTemplate.buildTemplate(SAMPLE_PREFIX, null,
                configuration.getIncludeKeyword(), configuration.getExcludeKeyword());
        this.register();
    }

    @Override
    public Map<String, List<MetricFamilySamples.Sample>> report() {
        Map<String, List<MetricFamilySamples.Sample>> metricFamilies =
                new HashMap<String, List<MetricFamilySamples.Sample>>(1024);

        for (int i = configuration.getKuduNodes().size() - 1; i >= 0; i--) {
            if (this.metricsPool.get(i) == null) {
                continue;
            }

            for (Map<?, ?> metricsJson : this.metricsPool.get(i)) {
                if (metricsJson == null) {
                    continue;
                }

                Object type = metricsJson.get("type");
                Object id = metricsJson.get("id");
                Object attributes = metricsJson.get("attributes");
                Object metrics = metricsJson.get("metrics");

                Map<String, String> labels = new ConcurrentHashMap<String, String>(32) {{
                    if (!StringUtils.isEmpty(type)) {
                        this.put("type", type.toString());
                    }
                    if (!StringUtils.isEmpty(id)) {
                        this.put("id", id.toString());
                    }
                    if (attributes instanceof Map) {
                        for (Map.Entry<?, ?> attr : ((Map<?, ?>) attributes).entrySet()) {
                            if (!StringUtils.isEmpty(attr.getKey()) && !StringUtils.isEmpty(attr.getValue())) {
                                this.put(attr.getKey().toString(), attr.getValue().toString());
                            }
                        }
                    }
                }};

                if (metrics instanceof Collection
                        && !((Collection<?>) metrics).isEmpty()) {

                    for (Object metric : (Collection<?>) metrics) {

                        List<MetricFamilySamples.Sample> metricSamples = new ArrayList<>();

                        try {
                            String metricName = ((Map<?, ?>) metric).get("name").toString();

                            for (Map.Entry<?, ?> entry : ((Map<?, ?>) metric).entrySet()) {

                                Object key = entry.getKey();
                                Object value = entry.getValue();

                                if (!key.toString().equals("name") && value instanceof Number) {

                                    if (key.toString().equals("value")) {
                                        MetricFamilySamples.Sample sample = this.template.generate(
                                                metricName, labels,
                                                Double.valueOf(value.toString())
                                        );
                                        if (sample != null) {
                                            metricSamples.add(sample);
                                        }

                                    } else {

                                        MetricFamilySamples.Sample sample = this.template.generate(
                                                metricName.concat(key.toString()), labels,
                                                Double.valueOf(value.toString())
                                        );

                                        if (sample != null) {
                                            metricSamples.add(sample);
                                        }

                                    }
                                }
                            }

                            if (metricSamples.size() != 0) {
                                if (!metricFamilies.containsKey(metricName)) {
                                    metricFamilies.put(metricName, new ArrayList<>(64));
                                }
                                metricFamilies.get(metricName).addAll(metricSamples);
                            }

                        } catch (Exception e) {
                            logger.warning(e.toString());
                        }

                    }

                }
            }

        }

        return metricFamilies;
    }

}