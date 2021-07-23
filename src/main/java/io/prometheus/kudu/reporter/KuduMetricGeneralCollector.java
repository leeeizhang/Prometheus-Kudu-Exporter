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

package io.prometheus.kudu.reporter;

import io.prometheus.client.Collector;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricPool;
import io.prometheus.kudu.util.LoggerUtils;
import io.prometheus.kudu.util.MetricSampleTemplate;
import io.prometheus.kudu.util.StringUtils;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collector as general class to parser json to metrics
 */
public class KuduMetricGeneralCollector extends Collector {
    protected static final Logger logger = LoggerUtils.Logger();

    private static final String SAMPLE_PREFIX = "kudu_";

    protected final KuduExporterConfiguration configuration;
    protected final KuduMetricPool<List<Map<?, ?>>> metricPool;

    /**
     * Constructor to init the Collector
     *
     * @param configuration exporter configuration
     * @param metricPool    metric pool to get metric resources
     */
    public KuduMetricGeneralCollector(
            KuduExporterConfiguration configuration,
            KuduMetricPool<List<Map<?, ?>>> metricPool) {
        this.configuration = configuration;
        this.metricPool = metricPool;
    }

    /**
     * Run collect to read pool metric resource and generate metric samples
     *
     * @return the key of map is label, value are metric list
     */
    public Map<String, List<MetricFamilySamples.Sample>> doCollect() {
        Map<String, List<MetricFamilySamples.Sample>> metricFamilies =
                new ConcurrentHashMap<>(1 << 16);

        MetricSampleTemplate template = MetricSampleTemplate.buildTemplate(
                SAMPLE_PREFIX,
                null,
                configuration.getMetricIncludeKeys(),
                configuration.getMetricExcludeKeys()
        );

        for (int i = configuration.getFetcherKuduNodes().size() - 1; i >= 0; i--) {
            if (this.metricPool.read(i) == null) {
                continue;
            }

            for (Map<?, ?> metricsJson : this.metricPool.read(i)) {
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
                            logger.warn("collector error.", e);
                        } finally {
                            logger.debug("collector parser json to metric error done.");
                        }

                    }

                }
            }

        }

        return metricFamilies;
    }

    /**
     * override collect function
     *
     * @return metric sample lists
     */
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