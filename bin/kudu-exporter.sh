java -jar $(dirname $(pwd))/lib/prometheus-kudu-exportor-1.0.0.jar \
     --config $(dirname $(pwd))/conf/kudu-exporter.yaml \
     --include $(dirname $(pwd))/conf/include-metrics \
     --exclude $(dirname $(pwd))/conf/exclude-metrics