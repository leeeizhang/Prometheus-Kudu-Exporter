package io.prometheus.kudu.reducer;

import com.google.gson.internal.LinkedTreeMap;
import io.prometheus.client.Collector;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.MetricSampleTemplate;
import io.prometheus.kudu.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class KuduMetricsReporterRunner extends Collector {

    private static final Logger logger = Logger.getLogger(KuduMetricsReporterRunner.class.getName());

    private static final String SAMPLE_PREFIX = "kudu_";

    private final KuduExporterConfiguration configuration;
    private final KuduMetricsPool<List<Map<?, ?>>> metricsPool;

    private final MetricSampleTemplate template;

    private KuduMetricsReporterRunner(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        this.configuration = configuration;
        this.metricsPool = metricsPool;
        this.template = MetricSampleTemplate.buildTemplate(SAMPLE_PREFIX, null,
                configuration.getIncludeKeyword(), configuration.getExcludeKeyword());
    }

    public static KuduMetricsReporterRunner builder(
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricsPool) {
        return new KuduMetricsReporterRunner(configuration, metricsPool);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> metricsResult = new ArrayList<>();

        for (int i = configuration.getKuduMetricURL().size() - 1; i >= 0; i--) {
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

                Map<String, String> labels = new ConcurrentHashMap<>(32) {{
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
                        && ((Collection<?>) metrics).size() != 0) {

                    for (Object metric : (Collection<?>) metrics) {

                        List<MetricFamilySamples.Sample> metricsSamples = new ArrayList<>();

                        try {
                            String metricName = ((Map<?, ?>) metric).get("name").toString();

                            for (Map.Entry<?, ?> entry : ((Map<?, ?>) metric).entrySet()) {
                                Object key = entry.getKey();
                                Object value = entry.getValue();

                                if (!key.toString().equals("name")) {
                                    if (key.toString().equals("value")) {
                                        MetricFamilySamples.Sample sample = this.template.generate(
                                                metricName, labels,
                                                Double.valueOf(value.toString())
                                        );
                                        if (sample != null) {
                                            metricsSamples.add(sample);
                                        }
                                    } else {
                                        MetricFamilySamples.Sample sample = this.template.generate(
                                                metricName.concat(key.toString()), labels,
                                                Double.valueOf(value.toString())
                                        );
                                        if (sample != null) {
                                            metricsSamples.add(sample);
                                        }
                                    }
                                }
                            }
                            if (metricsSamples.size() != 0) {
                                metricsResult.add(new MetricFamilySamples(
                                        metricName,
                                        Type.INFO,
                                        "help",
                                        metricsSamples
                                ));
                            }

                        } catch (Exception e) {
                            logger.warning(e.toString());
                        }
                    }

                }
            }
        }

        return metricsResult;
    }

}