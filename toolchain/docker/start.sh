cd /opt/prometheus/prometheus-2.55.1.linux-amd64
./prometheus > /dev/null &
cd /opt/grafana/grafana-v11.3.0
bin/grafana server > /dev/null &
cd /opt/decent-dev/application
bin/decent-dev

wait