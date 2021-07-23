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

package io.prometheus.kudu.fetcher;

import com.google.gson.Gson;
import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricsPool;
import io.prometheus.kudu.task.KuduExporterTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Fetch metric resources from kudu rest api
 */
public class KuduMetricRestFetcher extends KuduExporterTask<List<Map<?, ?>>> {
    private static final String RESPONSE_CHARSET = "UTF-8";

    private URL kuduRestURL;
    private Gson gson;

    /**
     * Constructor to init the Fetcher
     *
     * @param threadID      it should also be the index of kudu nodes
     * @param configuration exporter configuration
     * @param metricPool    metric pool to last metric resources
     */
    public KuduMetricRestFetcher(
            Integer threadID,
            KuduExporterConfiguration configuration,
            KuduMetricsPool<List<Map<?, ?>>> metricPool) {
        super(threadID, configuration, metricPool);
    }

    /**
     * Generate the rest api url and Gson
     *
     * @throws Exception
     */
    @Override
    protected void start() {
        try {
            StringBuilder includeMetricStr = new StringBuilder();
            for (int i = configuration.getMetricIncludeKeys().size() - 1; i >= 0; i--) {
                includeMetricStr.append(configuration.getMetricIncludeKeys().get(i).concat(i == 0 ? "" : ","));
            }
            kuduRestURL = new URL(String.format(
                    "http://%s/metrics?compact=1&metrics=%s",
                    configuration.getFetcherKuduNodes().get(threadID),
                    includeMetricStr
            ));
            gson = new Gson();
        } catch (MalformedURLException e) {
            logger.warn("malformed url for getting content.", e);
        } finally {
            logger.info(String.format("rest fetcher-%d start.", threadID));
        }
    }

    /**
     * Get metric resources json from rest api
     */
    @Override
    protected void process() {
        try (InputStreamReader reader = new InputStreamReader(
                this.kuduRestURL.openConnection().getInputStream(), RESPONSE_CHARSET)) {
            this.metricPool.write(this.threadID, this.gson.fromJson(reader, List.class));
        } catch (UnsupportedEncodingException e) {
            logger.warn("fetch kudu metric from Kudu Rest API failed for unsupported encoding contents.", e);
        } catch (IOException e) {
            logger.warn("fetch kudu metric from RestAPI failed .", e);
        } finally {
            logger.debug(String.format("rest fetcher-%d processed.", threadID));
        }
    }

    /**
     * Call when thread destroy
     */
    @Override
    protected void stop() {
        logger.info(String.format("rest fetcher-%d stop.", threadID));
    }

}
