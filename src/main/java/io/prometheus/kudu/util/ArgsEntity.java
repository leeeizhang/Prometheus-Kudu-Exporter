package io.prometheus.kudu.util;

import org.kohsuke.args4j.Option;

import java.io.Serializable;
import java.util.Objects;

public class ArgsEntity implements Serializable {

    @Option(name = "-c", aliases = "--config", required = true, usage = "./conf/kudu-exporter.yaml")
    private String configPath;

    @Option(name = "-i", aliases = "--include", required = true, usage = "./conf/include-metrics")
    private String includePath;

    @Option(name = "-e", aliases = "--exclude", required = true, usage = "./conf/exclude-metrics")
    private String excludePath;

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getIncludePath() {
        return includePath;
    }

    public void setIncludePath(String includePath) {
        this.includePath = includePath;
    }

    public String getExcludePath() {
        return excludePath;
    }

    public void setExcludePath(String excludePath) {
        this.excludePath = excludePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgsEntity that = (ArgsEntity) o;
        return Objects.equals(configPath, that.configPath) && Objects.equals(includePath, that.includePath) && Objects.equals(excludePath, that.excludePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configPath, includePath, excludePath);
    }

    @Override
    public String toString() {
        return "ArgsEntity{" +
                "configPath='" + configPath + '\'' +
                ", includePath='" + includePath + '\'' +
                ", excludePath='" + excludePath + '\'' +
                '}';
    }
}
