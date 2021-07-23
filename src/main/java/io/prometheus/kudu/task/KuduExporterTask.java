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

package io.prometheus.kudu.task;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

public abstract class KuduExporterTask<T> implements Runnable {
    protected static final Logger logger = LoggerUtils.Logger();

    protected final Integer threadID;
    protected final KuduExporterConfiguration configuration;
    protected final KuduMetricsPool<T> metricPool;

    /**
     * Protected Constructor to init the ExporterTask
     *
     * @param threadID      current thread id
     * @param configuration exporter configuration
     * @param metricPool    metric pool to store thread output
     */
    protected KuduExporterTask(
            Integer threadID,
            KuduExporterConfiguration configuration,
            KuduMetricsPool<T> metricPool) {
        this.threadID = threadID;
        this.configuration = configuration;
        this.metricPool = metricPool;
    }

    /**
     * This function will be call when thread is constructed
     *
     * @throws Exception
     */
    protected abstract void start() throws Exception;

    /**
     * This function will be call to process its task
     *
     * @throws Exception
     */
    protected abstract void process() throws Exception;

    /**
     * This function will be call when thread is destroyed
     *
     * @throws Exception
     */
    protected abstract void stop() throws Exception;

    /**
     * Start task jobs when thread in running
     */
    @Override
    public void run() {
        try {
            start();
            process();
            stop();
        } catch (Exception e) {
            logger.warn(String.format("thread-%d meet error.", this.threadID), e);
        }
    }

}