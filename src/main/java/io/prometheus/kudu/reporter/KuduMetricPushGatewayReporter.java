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
import io.prometheus.client.exporter.PushGateway;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricPool;
import io.prometheus.kudu.task.KuduExporterTask;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Push-Gateway Reporter
 */
public class KuduMetricPushGatewayReporter extends KuduExporterTask<List<Map<?, ?>>> {
    private Collector kuduMetricCollector;
    private PushGateway pushGateway;

    /**
     * Constructor to init the Reporter
     *
     * @param threadID      the id of current reporter thread
     * @param configuration exporter configuration
     * @param metricPool    metric pool to get metric resources
     */
    public KuduMetricPushGatewayReporter(
            Integer threadID,
            KuduExporterConfiguration configuration,
            KuduMetricPool<List<Map<?, ?>>> metricPool) {
        super(threadID, configuration, metricPool);
    }

    /**
     * Generate the PushGateway and Collector
     */
    @Override
    protected void start() {
        try {
            this.kuduMetricCollector = new KuduMetricGeneralCollector(configuration, metricPool);
            this.pushGateway = new PushGateway(configuration.getPushGatewayReporterHost());
            this.pushGateway.push(kuduMetricCollector, "kudu");
        } catch (IOException e) {
            logger.warn("push-gateway reporter start error.", e);
        } finally {
            logger.info("push-gateway reporter start.");
        }
    }

    /**
     * Report metrics into push-gateway periodically
     */
    @Override
    protected void process() {
        try {
            this.pushGateway.push(kuduMetricCollector, "kudu");
        } catch (IOException e) {
            logger.warn("fail to report metrics to push-gateway.", e);
        } finally {
            logger.debug("push-gateway reporter processed.");
        }
    }

    /**
     * Stop server when thread destroy
     */
    @Override
    protected void stop() {
        logger.info("push-gateway reporter stop.");
    }

}
