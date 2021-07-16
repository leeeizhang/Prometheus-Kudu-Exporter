package io.prometheus.kudu.util;

import io.prometheus.client.Collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetricSampleTemplate {

    private String metricPrefix;
    private List<String> labelNames;
    private List<String> labelValues;
    private String[] includeKeys;
    private String[] excludeKeys;

    public MetricSampleTemplate(
            String metricPrefix,
            List<String> labelNames,
            List<String> labelValues,
            String[] includeKeys,
            String[] excludeKeys) {
        this.metricPrefix = metricPrefix;
        this.labelNames = labelNames;
        this.labelValues = labelValues;
        this.includeKeys = includeKeys;
        this.excludeKeys = excludeKeys;
    }

    public static final MetricSampleTemplate buildTemplate(
            String prefix,
            Map<String, String> labels,
            String[] includeKeys,
            String[] excludeKeys) {
        List<String> labelNames = new ArrayList<>();
        List<String> labelValues = new ArrayList<>();
        if (labels != null && !labels.isEmpty()) {
            for (Map.Entry<String, String> label : labels.entrySet()) {
                labelNames.add(label.getKey());
                labelValues.add(label.getValue());
            }
        }
        return new MetricSampleTemplate(prefix, labelNames, labelValues, includeKeys, excludeKeys);
    }

    public final Collector.MetricFamilySamples.Sample generate(String appendName, Number value) {
        return this.generate(appendName, null, value);
    }

    private final boolean filterMetricName(String metricName) {

        for (int i = 0; i < excludeKeys.length; i++) {
            if (!"".equals(excludeKeys[i]) && metricName.contains(excludeKeys[i])) {
                return false;
            }
        }

        boolean includeFlag = (includeKeys.length == 0);
        for (int i = 0; i < includeKeys.length; i++) {
            if (!"".equals(includeKeys[i]) && metricName.contains(includeKeys[i])) {
                includeFlag = true;
                break;
            }
        }

        return includeFlag;
    }

    public final Collector.MetricFamilySamples.Sample generate(
            String appendName, Map<String, String> appendLabel, Number value) {

        String metricName = this.metricPrefix.concat(appendName);

        if (filterMetricName(metricName)) {
            List<String> labelNames = new ArrayList<>(this.labelNames);
            List<String> labelValues = new ArrayList<>(this.labelValues);
            if (appendLabel != null && !appendLabel.isEmpty()) {
                for (Map.Entry<String, String> label : appendLabel.entrySet()) {
                    labelNames.add(label.getKey());
                    labelValues.add(label.getValue());
                }
            }
            return new Collector.MetricFamilySamples.Sample(
                    metricName, labelNames, labelValues, value.doubleValue());
        }
        return null;
    }

}