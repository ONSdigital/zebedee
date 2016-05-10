package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.configuration.Configuration;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Created by iankent on 01/04/2016.
 */
public class InfluxDB {
    protected static org.influxdb.InfluxDB influxDB = null;
    protected static String pingDBName = "ping";
    protected static ExecutorService pool = Executors.newSingleThreadExecutor();

    static {
        try {
            if (Configuration.isInfluxReportingEnabled()) {
                logInfo("creating influxdb instance").log();
                influxDB = InfluxDBFactory.connect(Configuration.getInfluxDBHost(), "root", "root");

                influxDB.createDatabase(pingDBName);
            }
        } catch (Exception e) {
            logError(e, "error initialising InfluxDB class").log();
            throw e;
        }
    }

    public static void Ping(long ms) {
        if (!Configuration.isInfluxReportingEnabled()) return;

        pool.submit(() -> {
            BatchPoints batchPoints = BatchPoints
                    .database(pingDBName)
                    .tag("async", "true")
                    .retentionPolicy("default")
                    .consistency(org.influxdb.InfluxDB.ConsistencyLevel.ALL)
                    .build();
            Point point1 = Point.measurement("ping")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .field("ping", ms)
                    .build();

            batchPoints.point(point1);
            influxDB.write(batchPoints);
        });
    }
}
