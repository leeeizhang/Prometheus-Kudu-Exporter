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

package io.prometheus.kudu.sink;

import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Concurrent-limited pool to store metric resources
 *
 * @param <T> type of pool resources
 */
public class KuduMetricPool<T> implements Serializable {

    private static final Logger logger = LoggerUtils.Logger();

    private final Map<Integer, T> pool;

    /**
     * Readers–writers Problem, for more details:
     * https://en.wikipedia.org/wiki/Readers-writers_problem
     */
    private Integer readerCount;
    private Semaphore resourceMutex;
    private Semaphore readCountMutex;
    private Semaphore serviceQueueMutex;

    /**
     * Private constructor to init the Pool
     */
    private KuduMetricPool() {
        this.pool = new ConcurrentHashMap<>(1024);
        readerCount = 0;
        serviceQueueMutex = new Semaphore(1);
        readCountMutex = new Semaphore(1);
        resourceMutex = new Semaphore(1);
    }

    /**
     * Build general instance
     *
     * @param <T> type of pool resources
     * @return Pool instance
     */
    public static <T> KuduMetricPool<T> build() {
        return new KuduMetricPool<>();
    }

    /**
     * Concurrent-limited write source into pool
     *
     * @param id     key
     * @param source value
     * @return input value for chainable call
     */
    public T write(Integer id, T source) {
        try {
            serviceQueueMutex.acquire();
            resourceMutex.acquire();
            this.pool.put(id, source);
            resourceMutex.release();
            serviceQueueMutex.release();
            return source;
        } catch (InterruptedException e) {
            logger.warn("read-write semaphore in metric pool meet concurrency control exception.");
        }
        return null;
    }

    /**
     * Concurrent-limited read source from pool
     *
     * @param id key
     * @return value
     */
    public T read(Integer id) {
        T result = null;
        try {
            serviceQueueMutex.acquire();
            readCountMutex.acquire();
            if (readerCount == 0) {
                resourceMutex.acquire();
            }
            readerCount++;
            readCountMutex.release();
            serviceQueueMutex.release();

            if (pool.containsKey(id)) {
                result = pool.get(id);
            }

            readCountMutex.acquire();
            readerCount--;
            if (readerCount == 0) {
                resourceMutex.release();
            }
            readCountMutex.release();

        } catch (InterruptedException e) {
            logger.warn("read-write semaphore in metric pool meet concurrency control exception.");
        }
        return result;
    }

    /**
     * Get the size of pool
     *
     * @return size
     */
    public int size() {
        return this.pool.size();
    }

}
