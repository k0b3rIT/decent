Decent investment task
=========

The purpose of this project is to create a simple orderbook application that will maintain orderbooks for different symbols and levels. The application will be able to receive orderbook updates and maintain the orderbook for the given symbol and level.

Features:
- Can maintain orderbook for multiple symbols and levels at the same time.
- You can start/stop orderbook maintenance for a given symbol and level via a REST API.
- Robust against network failures (try to reconnect indefinitely).
- Robust against invalid orderbook updates. (missing updates)

How to execute
-----------
You have 4 options to execute the application

### Execute from prebuilt docker image, available on dockerhub (suggested way)
https://hub.docker.com/r/k0b3rit/decent/tags

```
docker run --platform linux/amd64 --name decent --rm -p 9090:9090 -p 3000:3000 -p 8080:8080 k0b3rit/decent
```

### Execute from docker image built from source locally

Steps:

1. ##### Build the project

Use Java17 or greater
```
./gradlew clean build
```

2. ##### Build the docker image (execute it from the project root directory)
```
docker build -f toolchain/docker/Dockerfile -t k0b3rit/decent . --platform linux/amd64
```

3. ##### Run the docker image
```
docker run --platform linux/amd64 --name decent --rm -p 9090:9090 -p 3000:3000 -p 8080:8080 k0b3rit/decent
```

### Execute from source (The Grafana and prometheus will not be available in this case)

Use Java17 or greater
```
./gradlew clean application:bootRun
```

### Execute from IDE (The Grafana and prometheus will not be available in this case)
Import the project in your favorite IDE and run the main class `Application.java` in the application module.


How to use the app
---------
The application can maintain multiple orderbooks for different symbols and levels at the same time.\
I would suggest to start a couple of orderbook maintain request for different symbols and levels and let it run for 5 min.\
To do that you can use the swagger ui to start/stop orderbook maintenance for a given symbol and level.\
Provide the symbol in the following format: `BTCUSDT` (only 1 at a time, you have to execute the request multiple times for different symbols)

### Swagger ui (REST API):
http://localhost:8080/swagger-ui/index.html

The application will produce the result into the stdout in the requested format. (if you maintain multiple orderbooks at the same time, the output will be mixed)


### Metrics
The application will also expose the following metrics:
- orderbook_updates_total: The total number of orderbook updates received.

In addition to the above metrics, the application will also expose various JVM metrics.\
The raw metrics are available on the following endpoint:\
http://localhost:8080/actuator/prometheus

To visualize the metrics you can access the following predefined grafana dashboard.

### Grafana:
http://localhost:3000

The dashboard is available under the following path (no auth needed):\
(Let the application run for a couple of minutes to see the metrics)\
Dashboards -> applications -> Decent dev

### Prometheus:
http://localhost:9090
Act as a timeseries database to store the metrics.


Notes
---------
I have tested the network robustness by turning off the network interface for a couple of seconds and the application was able to reconnect and continue to work as expected.\
I have tested the application against inconsistent orderbook updates by raising the same exception manually as the invalid orderbook update would raise. The application was able to recover and continue to work as expected.\
