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
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.task.KuduExporterTask;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Standalone (Local) Reporter
 */
public class KuduMetricLocalReporter extends KuduExporterTask<List<Map<?, ?>>> {
    private Collector kuduMetricCollector;
    private HTTPServer httpServer;

    /**
     * Constructor to init the Reporter
     *
     * @param threadID      the id of current reporter thread
     * @param configuration exporter configuration
     * @param metricPool    metric pool to get metric resources
     */
    public KuduMetricLocalReporter(
            Integer threadID,
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricPool) {
        super(threadID, configuration, metricPool);
    }

    /**
     * Generate the HttpServer and Collector, register self in Standalone HttpServer
     */
    @Override
    protected void start() {
        try {
            this.httpServer = new HTTPServer(configuration.getLocalReporterPort(), true);
            this.kuduMetricCollector = new KuduMetricGeneralCollector(configuration, metricPool);
            this.kuduMetricCollector.register();
        } catch (IOException e) {
            logger.warn("standalone reporter http server start error.", e);
        }
    }

    /**
     * Nothing need to do, it will call collector when Prometheus get metrics periodically
     */
    @Override
    protected void process() {
    }

    /**
     * Stop server when thread destroy
     */
    @Override
    protected void stop() {
        httpServer.stop();
    }

}