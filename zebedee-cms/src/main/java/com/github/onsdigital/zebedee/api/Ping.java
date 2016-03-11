package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.PingRequest;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by thomasridd on 14/07/15.
 */
@Api
public class Ping {

    ExecutorService pool = Executors.newSingleThreadExecutor();

    public static void main(String[] args) throws InterruptedException {

        while (true) {
            Random rn = new Random();

            InfluxDB influxDB = InfluxDBFactory.connect("http://127.0.0.1:8086", "root", "root");
            String dbName = "ping";
            influxDB.createDatabase(dbName);

            BatchPoints batchPoints = BatchPoints
                    .database(dbName)
                    .tag("async", "true")
                    .retentionPolicy("default")
                    .consistency(InfluxDB.ConsistencyLevel.ALL)
                    .build();
            Point point1 = Point.measurement("ping")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .field("ping", rn.nextInt(5000) + 40)
                    .build();

            batchPoints.point(point1);
            influxDB.write(batchPoints);

            Thread.sleep(1000);
        }
    }

    /**
     * Sends a message to a user session requiring it to stay alive
     * <p>
     * Returns true if the session is alive, false otherwise
     */
    @POST
    public boolean ping(HttpServletRequest request, HttpServletResponse response, PingRequest pingRequest) throws IOException {
        pool.submit(() -> {

            System.out.println("lastPingTime = " + pingRequest.lastPingTime);

            if (pingRequest.lastPingTime != null && pingRequest.lastPingTime > 0) {

                InfluxDB influxDB = InfluxDBFactory.connect("http://influxdb", "root", "root");
                String dbName = "ping";
                influxDB.createDatabase(dbName);

                BatchPoints batchPoints = BatchPoints
                        .database(dbName)
                        .tag("async", "true")
                        .retentionPolicy("default")
                        .consistency(InfluxDB.ConsistencyLevel.ALL)
                        .build();
                Point point1 = Point.measurement("ping")
                        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .field("ping", pingRequest.lastPingTime)
                        .build();

                batchPoints.point(point1);
                influxDB.write(batchPoints);
            }
        });

        return true;
    }
}