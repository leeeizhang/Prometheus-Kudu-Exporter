# Prometheus-Kudu-Exporter

![license](https://img.shields.io/github/license/ContainerSolutions/locust_exporter.svg) ![stars](https://img.shields.io/github/stars/magicdevilzhang/prometheus-kudu-exporter) ![size](https://img.shields.io/github/repo-size/magicdevilzhang/prometheus-kudu-exporter?color=orange&label=size)

Kudu Exporter, fetching metrics by multi-thread from [Kudu Rest API](https://kudu.apache.org/docs/administration.html#_collecting_metrics_via_http) and reporting them by standalone or push-gateway, can be applied high-availably for Prometheus.

1. [Installation](#Installation)
   1. [Compile](#Compile)
   2. [Configuration](#Configuration)
   3. [Run Server](#Run-Server)
2. [Reference](#Reference)
   1. [Reporting Methods](#Reporting-Methods)
   2. [Metrics Filter](#Metrics-Filter)
3. [Contributing](#Contributing)
4. [License](#License)

# Installation

The project can be deployed in many ways, and we suggest as following.

## Compile

Kudu Exporter, finished by Java, need to compile before deploying. Download this project and compile it after maven and git is installed.

```shell
# Install Git and Maven
$ sudo apt-get update
$ sudo apt install git maven

# Clone this Project
$ sudo git clone https://github.com/magicdevilzhang/prometheus-kudu-exporter.git
$ sudo cd ./prometheus-kudu-exporter

# Compile by Maven
$ sudo mvn clean package assembly:single
```

## Configuration

Kudu Exporter support **Standalone** or **Push-Gateway** deployment. Standalone deployment can provide HTTP server that fetching metric resources into metric pool in advance to wait Prometheus getting metrics periodically. Alternatively, the Push-Gateway deployment, as we suggest, can reporting standard metrics to Prometheus Push Gateway initiatively and periodically. 

Here is some examples to configure report method in Kudu Exporter by create or edit `./conf/kudu-exporter.yml`.

- Standalone Deployment (Local Reporter)

```yaml
# Define the exporter classname
prom.kudu.metric.fetcher-classname: io.prometheus.kudu.fetcher.KuduMetricRestFetcher
prom.kudu.metric.reporter-classname: io.prometheus.kudu.reporter.KuduMetricLocalReporter

# Define the fetcher configuration
prom.kudu.metric.kudu-nodes: [ 127.0.0.1:8051, 127.0.0.1:8050 ]
prom.kudu.metric.fetch-interval: 10000

# Define the reporter configuration
prom.kudu.metric.reporter-port: 9055
```

- Push-Gateway Deployment ***(Recommend)***

```yaml
# Define the exporter classname
prom.kudu.metric.fetcher-classname: io.prometheus.kudu.fetcher.KuduMetricRestFetcher
prom.kudu.metric.reporter-classname: io.prometheus.kudu.reporter.KuduMetricPushGatewayReporter

# Define the fetcher configuration
prom.kudu.metric.kudu-nodes: [ 127.0.0.1:8051, 127.0.0.1:8050 ]
prom.kudu.metric.fetch-interval: 10000

# Define the reporter configuration
prom.kudu.metric.pushgateway: 127.0.0.1:9091
prom.kudu.metric.push-interval: 20000
```

## Run Server

Kudu Exporter can be run as multi-thread with less system resource after compiling and configuring correctly. 

```shell
$ nohup ./bin/kudu-exporter.sh &
```

Once start successfully, please visit: 

- Standalone Reporter (Local Reporter): http://localhost:9055/metrics.
- Push-Gateway Reporter: http://pushgateway-ip:port/metrics/job/kudu.

Alternatively, if you need to run Kudu Exporter jar manually, following this method.

```shell
$ java -jar ./lib/prometheus-kudu-exporter-x.x.x.jar \
       --config ./conf/kudu-exporter.yml \
       --include ./conf/include-metrics \
       --exclude ./conf/exclude-metrics
```

## Reference

The directory `conf` is the configuration of Kudu Exporter. We try our best to balance the simplified configuration, as Prometheus official suggested, and custom features. Although Kudu Exporter can be effectively deployed as above-mentioned steps, you can also follow next guideline if you have specially requirements.

*Note: More function requirements can be met by email us or create issue in GitHub.*

## Reporting Methods

**Standalone** or **Push-Gateway** deployment are provided respectively, which you can configure them as above-mentioned [Configuration](#Configuration). Push-Gateway is what we suggested if each server node has different Prometheus exporter such as node exporter, Flink exporter, MySQL exporter, etc. You need to let different Prometheus exporters report their metrics into one collector(like [Push-Gateway](https://github.com/prometheus/pushgateway))  to reduce the listening port using.

Further more, the meaning of `./conf/kudu-exporter.yml` is as following list.

|              parameter              |     type     |                       default                       |                            detail                            |
| :---------------------------------: | :----------: | :-------------------------------------------------: | :----------------------------------------------------------: |
| prom.kudu.metric.fetcher-classname  |    string    |  io.prometheus.kudu.fetcher.KuduMetricRestFetcher  | Fetcher aim at getting metrics from kudu. No more fetchers supported currently. |
| prom.kudu.metric.reporter-classname |    string    | io.prometheus.kudu.reporter.KuduMetricLocalReporter |         [Standalone / Push-Gateway](#Configuration)          |
|     prom.kudu.metric.kudu-nodes     | list(string) |                        null                         |         Try visit http://ip:port/metrics to verify.          |
|   prom.kudu.metric.fetch-interval   |     long     |                     10000 (ms)                      |         Fetcher take this as a cycle to get metrics.         |
|    prom.kudu.metric.pushgateway     |    string    |                        null                         |                  Push-Gateway host and port                  |
|   prom.kudu.metric.push-interval    |     long     |                     20000 (ms)                      |        Reporter take this as a cycle to put metrics.         |
|   prom.kudu.metric.reporter-port    |   integer    |                        9055                         | Only KuduMetricLocalReporter support it to get metrics via http://127.0.0.1:9055/ |

## Metrics Filter

Kudu support an ocean of metrics, you can filter them via keyword by editing `./conf/include-metrics` and `./conf/exclude-metrics`. Following [Kudu Metrics Reference](https://kudu.apache.org/docs/metrics_reference.html) to find the metrics you need.

Here is what we suggest:

- on_disk_data_size
- on_disk_size
- rows_deleted
- rows_inserted
- rows_updated
- rows_upserted
- generic_heap_size
- generic_current_allocated_bytes
- block_manager_total_bytes_written
- block_manager_total_bytes_read
- threads_running
- tablets_num_running

# Contributing

Pull requests are more than welcome!

We recommend having a look at the [Prometheus Exporter Design Document](https://prometheus.io/docs/instrumenting/writing_exporters/) before contributing.

# License

Copyright 2021 RyanCheung98@163.com

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

