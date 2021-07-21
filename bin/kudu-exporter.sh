#!/usr/bin/env bash

target="$0"

iteration=0
while [ -L "$target" ]; do
    if [ "$iteration" -gt 100 ]; then
        break
    fi
    ls=$(ls -ld -- "$target")
    target=$(expr "$ls" : '.* -> \(.*\)$')
    iteration=$((iteration + 1))
done

bin=$(dirname "$target")

PROMETHEUS_KUDU_EXPORTER_HOME_DIR=$(cd "$bin/.." || exit; pwd -P)
PROMETHEUS_KUDU_EXPORTER_CONF_DIR="$PROMETHEUS_KUDU_EXPORTER_HOME_DIR"/conf
PROMETHEUS_KUDU_EXPORTER_LIB_DIR="$PROMETHEUS_KUDU_EXPORTER_HOME_DIR"/lib

PROMETHEUS_KUDU_EXPORTER_CONF="$PROMETHEUS_KUDU_EXPORTER_CONF_DIR"/kudu-exporter.yaml
PROMETHEUS_KUDU_EXPORTER_CONF_INCLUDE="$PROMETHEUS_KUDU_EXPORTER_CONF_DIR"/include-metrics
PROMETHEUS_KUDU_EXPORTER_CONF_EXCLUDE="$PROMETHEUS_KUDU_EXPORTER_CONF_DIR"/exclude-metrics

PROMETHEUS_KUDU_EXPORTER_JAR=$(find "$PROMETHEUS_KUDU_EXPORTER_LIB_DIR" -regex ".*.jar")

PROMETHEUS_KUDU_EXPORTER_CLASSPATH=""
while read -d '' -r jarfile ; do
    if [[ "$PROMETHEUS_KUDU_EXPORTER_CLASSPATH" == "" ]]; then
        PROMETHEUS_KUDU_EXPORTER_CLASSPATH="$jarfile";
    else
        PROMETHEUS_KUDU_EXPORTER_CLASSPATH="$PROMETHEUS_KUDU_EXPORTER_CLASSPATH":"$jarfile"
    fi
done < <(find "$PROMETHEUS_KUDU_EXPORTER_LIB_DIR" ! -type d -name '*.jar' -print0 | sort -z)

echo $PROMETHEUS_KUDU_EXPORTER_CLASSPATH

if [ -n "$PROMETHEUS_KUDU_EXPORTER_JAR" ]; then

    exec java -classpath "${PROMETHEUS_KUDU_EXPORTER_CLASSPATH}" io.prometheus.kudu.KuduExporter "$@" \
      --config "$PROMETHEUS_KUDU_EXPORTER_CONF" \
      --include "$PROMETHEUS_KUDU_EXPORTER_CONF_INCLUDE" \
      --exclude "$PROMETHEUS_KUDU_EXPORTER_CONF_EXCLUDE"

else

    (>&2 echo "[ERROR] Prometheus Kudu Exporter JAR file should be located in $PROMETHEUS_KUDU_EXPORTER_LIB_DIR.")
    exit 1

fi