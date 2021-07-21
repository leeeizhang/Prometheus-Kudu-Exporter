package io.prometheus.kudu.sink;

import com.sun.org.slf4j.internal.Logger;
import io.prometheus.kudu.util.LoggerUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public class KuduMetricsPool<T> implements Serializable {

    private static final Logger logger = LoggerUtils.Logger();

    private final Map<Integer, Future<T>> repoFuture;

    private Integer readerCount;
    private Semaphore mutex;
    private Semaphore readerAndWriterMutex;
    private Semaphore writerMutex;


    private KuduMetricsPool() {
        this.repoFuture = new ConcurrentHashMap<>(1024);
        readerCount = 0;
        mutex = new Semaphore(1);
        readerAndWriterMutex = new Semaphore(1);
        writerMutex = new Semaphore(1);
    }

    public static <T> KuduMetricsPool<T> build() {
        return new KuduMetricsPool<>();
    }

    public void write(Integer id, Future<T> future) {
        try {
            writerMutex.acquire();
            readerAndWriterMutex.acquire();
            this.repoFuture.put(id, future);
            readerAndWriterMutex.release();
            writerMutex.release();
        } catch (InterruptedException e) {
            logger.warn("Read-Write lock in MetricPool Concurrency Control exception.");
        }
    }

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

            if (repoFuture.containsKey(id)) {
                result = repoFuture.get(id).get();
            }

            mutex.acquire();
            readerCount--;
            if (readerCount == 0) {
                readerAndWriterMutex.release();
            }
            mutex.release();

        } catch (InterruptedException e) {
            logger.warn("Read-Write lock in MetricPool Concurrency Control exception.");
        } catch (ExecutionException e) {
            logger.warn("Cannot get metrics from MetricPool.");
        }
        return result;
    }

}
