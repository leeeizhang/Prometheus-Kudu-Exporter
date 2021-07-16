package io.prometheus.kudu.producer;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class KuduMetricsFetcher implements Callable<List<Map<?, ?>>> {

    private final static String RESPONSE_CHARSET = "UTF-8";

    private final URL url;
    private final Gson gson;

    public KuduMetricsFetcher(URL url, Gson gson) {
        this.url = url;
        this.gson = gson;
    }

    public List<Map<?, ?>> fetch() throws IOException {
        URLConnection connection = this.url.openConnection();
        BufferedReader inStream = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), RESPONSE_CHARSET));
        return this.gson.fromJson(inStream, List.class);
    }

    @Override
    public List<Map<?, ?>> call() throws Exception {
        return fetch();
    }

}
