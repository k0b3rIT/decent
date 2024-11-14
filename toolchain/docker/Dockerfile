FROM --platform=linux/amd64 ubuntu:22.04

RUN apt-get update && apt-get install -y wget unzip openjdk-17-jdk

RUN mkdir -p /opt/prometheus
WORKDIR /opt/prometheus
RUN wget https://github.com/prometheus/prometheus/releases/download/v2.55.1/prometheus-2.55.1.linux-amd64.tar.gz
RUN tar -xzf prometheus-2.55.1.linux-amd64.tar.gz
RUN mkdir -p /opt/grafana
WORKDIR /opt/grafana
RUN wget https://dl.grafana.com/enterprise/release/grafana-enterprise-11.3.0.linux-amd64.tar.gz
RUN tar -xzf grafana-enterprise-11.3.0.linux-amd64.tar.gz

COPY toolchain/docker/defaults.ini /opt/grafana/grafana-v11.3.0/conf/defaults.ini
COPY toolchain/docker/prometheus_datasource.yml /opt/grafana/grafana-v11.3.0/conf/provisioning/datasources/prometheus.yml
COPY toolchain/docker/prometheus.yml /opt/prometheus/prometheus-2.55.1.linux-amd64/prometheus.yml
COPY toolchain/docker/dashboards.yaml /opt/grafana/grafana-v11.3.0/conf/provisioning/dashboards/dashboards.yaml
COPY toolchain/docker/dashboard.json /opt/dashboards/applications/dashboard.json

EXPOSE 9090
EXPOSE 3000
EXPOSE 8080

RUN mkdir -p /opt/decent-dev
COPY application/build/distributions/application.zip /opt/decent-dev/application.zip
WORKDIR /opt/decent-dev

RUN unzip application.zip

COPY toolchain/docker/start.sh /opt/start.sh

CMD /opt/start.sh