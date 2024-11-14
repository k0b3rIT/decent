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
- market_data_process_time: histogram of the time it takes to process the market data (updates from the server) by market data observer
- market_data: counter of the processed market data by stream
- observer_executor_task_count: gauge of the number of tasks in the observer executor 

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


Notes on robustness testing
---------
I have tested the network robustness by turning off the network interface for a couple of seconds and the application was able to reconnect and continue to work as expected.\
I have tested the application against inconsistent orderbook updates by raising the same exception manually as the invalid orderbook update would raise. The application was able to recover and continue to work as expected.\
You can uncomment the following lines to see the inconsistent orderbook update handling in action:\ (the client restart will appear on the grafana dashboard too)
https://github.com/k0b3rIT/decent/blob/50de3e0653b5b98187255d89f8c9b6d9a59d8366/service/src/main/java/com/k0b3rit/service/orderbookmaintainer/LocalOrderbookMaintainer.java#L67


Notes on the implementation
---------
The implementation is more generic than it should be for this task.\
It designed to be able to extend to support multiple exchanges, different kind of market data streams, and multiple data observers.

### Main components

#### MarketDataObserver
An entity that consumes the incoming market data.\

#### MarketDataProvider
The MarketDataProvider has three main responsibilities.
- Maintaining the list of the observed streams (MarketDataIdentifier) and the associated observers. (Who needs what) (There can be multiple observers for the same stream)
- Providing a dedicated executor for each MarketDataObserver to act as worker thread.\
- Distribute the incoming market data to the associated observers.\
By having a dedicated worker thread we can offload the processing of the incoming market data to a separate thread and avoid blocking the websocket client thread and incoming market data. We can also ensure that the individual observers are not affecting each other.\
The executor's task pool act as a buffer for the incoming market data.\ (I expose a metric to monitor the number of tasks in the executor pool, if it is constantly high, it means that the observer is not able to keep up with the incoming market data and we should consider to optimize the observer logic)

#### LocalOrderbookMaintainer
This is the main component that handles the orderbook updates at the end of the pipeline.\
By abstracting away the websocket client and networking the core logic remains clean and easy to maintain.\