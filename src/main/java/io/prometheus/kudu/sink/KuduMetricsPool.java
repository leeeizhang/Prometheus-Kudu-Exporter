package io.prometheus.kudu.sink;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class KuduMetricsPool<T> implements Serializable {

    private static final Logger logger = Logger.getLogger(KuduMetricsPool.class.getName());

    private final Map<Integer, Future<T>> repoFuture;
    private final Map<Integer, T> repoHistory;

    public KuduMetricsPool() {
        this.repoFuture = new ConcurrentHashMap<>(1024);
        this.repoHistory = new ConcurrentHashMap<>(1024);
    }

    public void put(Integer id, Future<T> future) {
        this.repoFuture.put(id, future);
    }

    public T get(Integer id) {
        if (repoFuture.containsKey(id)) {
            Future<T> future = repoFuture.get(id);
            try {
                T entity = future.get();
                if (entity != null) {
                    repoHistory.put(id, entity);
                    return entity;
                } else {
                    throw new Exception();
                }
            } catch (Exception e) {
                logger.warning(e.toString());
                return repoHistory.getOrDefault(id, null);
            }
        }
        return null;
    }

}
