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

    private Integer readerCount;
    private Semaphore mutex;
    private Semaphore readerAndWriterMutex;
    private Semaphore writerMutex;

    /**
     * Private constructor to init the Pool
     */
    private KuduMetricPool() {
        this.pool = new ConcurrentHashMap<>(1024);
        readerCount = 0;
        mutex = new Semaphore(1);
        readerAndWriterMutex = new Semaphore(1);
        writerMutex = new Semaphore(1);
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
            writerMutex.acquire();
            readerAndWriterMutex.acquire();
            this.pool.put(id, source);
            readerAndWriterMutex.release();
            writerMutex.release();
            return source;
        } catch (InterruptedException e) {
            logger.warn("Read-Write lock in MetricPool Concurrency Control exception.");
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
            writerMutex.acquire();
            mutex.acquire();
            if (readerCount == 0) {
                readerAndWriterMutex.acquire();
            }
            readerCount++;
            mutex.release();
            writerMutex.release();

            if (pool.containsKey(id)) {
                result = pool.get(id);
            }

            mutex.acquire();
            readerCount--;
            if (readerCount == 0) {
                readerAndWriterMutex.release();
            }
            mutex.release();

        } catch (InterruptedException e) {
            logger.warn("Read-Write lock in MetricPool Concurrency Control exception.");
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
