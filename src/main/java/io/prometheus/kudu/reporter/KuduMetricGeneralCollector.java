package io.prometheus.kudu.reporter;

import io.prometheus.client.Collector;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricPool;
import io.prometheus.kudu.util.LoggerUtils;
import io.prometheus.kudu.util.MetricSampleTemplate;
import io.prometheus.kudu.util.StringUtils;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KuduMetricGeneralCollector extends Collector {
    private static final Logger logger = LoggerUtils.Logger();

    private static final String SAMPLE_PREFIX = "kudu_";

    protected final KuduExporterConfiguration configuration;
    protected final KuduMetricPool<List<Map<?, ?>>> metricsPool;

    public KuduMetricGeneralCollector(
            KuduExporterConfiguration configuration,
            KuduMetricPool<List<Map<?, ?>>> metricsPool) {
        this.configuration = configuration;
        this.metricsPool = metricsPool;
    }

    public Map<String, List<MetricFamilySamples.Sample>> doCollect() {
        Map<String, List<MetricFamilySamples.Sample>> metricFamilies =
                new HashMap<>(1024);

        MetricSampleTemplate template = MetricSampleTemplate.buildTemplate(
                SAMPLE_PREFIX,
                null,
                configuration.getMetricIncludeKeys(),
                configuration.getMetricExcludeKeys()
        );

        for (int i = configuration.getFetcherKuduNodes().size() - 1; i >= 0; i--) {
            if (this.metricsPool.read(i) == null) {
                continue;
            }

            for (Map<?, ?> metricsJson : this.metricsPool.read(i)) {
                if (metricsJson == null) {
                    continue;
                }

                Object type = metricsJson.get("type");
                Object id = metricsJson.get("id");
                Object attributes = metricsJson.get("attributes");
                Object metrics = metricsJson.get("metrics");
                Object hostIP = configuration.getFetcherKuduNodes().get(i);

                Map<String, String> labels = new ConcurrentHashMap<String, String>(32) {{
                    if (!StringUtils.isEmpty(hostIP)) {
                        this.put("host_ip", hostIP.toString());
                    }
                    if (!StringUtils.isEmpty(type)) {
                        this.put("type", type.toString());
                    }
                    if (!StringUtils.isEmpty(id)) {
                        this.put("id", id.toString());
                    }
                    if (attributes instanceof Map) {
                        for (Entry<?, ?> attr : ((Map<?, ?>) attributes).entrySet()) {
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
                                        MetricFamilySamples.Sample sample = template.generate(
                                                metricName, labels,
                                                Double.valueOf(value.toString())
                                        );
                                        if (sample != null) {
                                            metricSamples.add(sample);
                                        }

                                    } else {

                                        MetricFamilySamples.Sample sample = template.generate(
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
                            logger.warn(String.format("Reporter meet issues %s.", e.getCause()));
                        }

                    }

                }
            }

        }

        return metricFamilies;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> metricsResult = new ArrayList<>();
        for (Map.Entry<String, List<MetricFamilySamples.Sample>> entry : doCollect().entrySet()) {
            metricsResult.add(new MetricFamilySamples(
                    entry.getKey(),
                    Type.GAUGE,
                    "",
                    entry.getValue()
            ));
        }
        return metricsResult;
    }
}