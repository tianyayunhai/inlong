/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.audit.service;

import org.apache.inlong.audit.cache.HalfHourCache;
import org.apache.inlong.audit.cache.HourCache;
import org.apache.inlong.audit.cache.TenMinutesCache;
import org.apache.inlong.audit.channel.DataQueue;
import org.apache.inlong.audit.config.Configuration;
import org.apache.inlong.audit.entities.AuditCycle;
import org.apache.inlong.audit.entities.SinkConfig;
import org.apache.inlong.audit.entities.SourceConfig;
import org.apache.inlong.audit.sink.CacheSink;
import org.apache.inlong.audit.sink.JdbcSink;
import org.apache.inlong.audit.source.JdbcSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static org.apache.inlong.audit.config.ConfigConstants.DEFAULT_CLICKHOUSE_DRIVER;
import static org.apache.inlong.audit.config.ConfigConstants.DEFAULT_DAILY_SUMMARY_STAT_BACK_TIMES;
import static org.apache.inlong.audit.config.ConfigConstants.DEFAULT_DATA_QUEUE_SIZE;
import static org.apache.inlong.audit.config.ConfigConstants.DEFAULT_REALTIME_SUMMARY_STAT_BACK_TIMES;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_CLICKHOUSE_DRIVER;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_CLICKHOUSE_JDBC_URL;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_CLICKHOUSE_PASSWORD;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_CLICKHOUSE_USERNAME;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_DAILY_SUMMARY_STAT_BACK_TIMES;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_DATA_QUEUE_SIZE;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_DEFAULT_MYSQL_DRIVER;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_MYSQL_DRIVER;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_MYSQL_JDBC_URL;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_MYSQL_PASSWORD;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_MYSQL_USERNAME;
import static org.apache.inlong.audit.config.ConfigConstants.KEY_REALTIME_SUMMARY_STAT_BACK_TIMES;
import static org.apache.inlong.audit.config.SqlConstants.DEFAULT_CLICKHOUSE_SOURCE_QUERY_SQL;
import static org.apache.inlong.audit.config.SqlConstants.DEFAULT_MYSQL_SINK_INSERT_DAY_SQL;
import static org.apache.inlong.audit.config.SqlConstants.DEFAULT_MYSQL_SINK_INSERT_TEMP_SQL;
import static org.apache.inlong.audit.config.SqlConstants.DEFAULT_MYSQL_SOURCE_QUERY_TEMP_SQL;
import static org.apache.inlong.audit.config.SqlConstants.KEY_CLICKHOUSE_SOURCE_QUERY_SQL;
import static org.apache.inlong.audit.config.SqlConstants.KEY_MYSQL_SINK_INSERT_DAY_SQL;
import static org.apache.inlong.audit.config.SqlConstants.KEY_MYSQL_SINK_INSERT_TEMP_SQL;
import static org.apache.inlong.audit.config.SqlConstants.KEY_MYSQL_SOURCE_QUERY_TEMP_SQL;

/**
 * Etl service aggregate the data from the data source and store the aggregated data to the target storage.
 */
public class EtlService {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcSource.class);
    private JdbcSource mysqlSourceOfTemp;
    private JdbcSource mysqlSourceOfTenMinutesCache;
    private JdbcSource mysqlSourceOfHalfHourCache;
    private JdbcSource mysqlSourceOfHourCache;
    private JdbcSink mysqlSinkOfDay;
    private JdbcSource clickhouseSource;
    private JdbcSink mysqlSinkOfTemp;
    private CacheSink cacheSinkOfTenMinutesCache;
    private CacheSink cacheSinkOfHalfHourCache;
    private CacheSink cacheSinkOfHourCache;
    private final int queueSize;
    private final int statBackTimes;

    public EtlService() {
        queueSize = Configuration.getInstance().get(KEY_DATA_QUEUE_SIZE,
                DEFAULT_DATA_QUEUE_SIZE);
        statBackTimes = Configuration.getInstance().get(KEY_REALTIME_SUMMARY_STAT_BACK_TIMES,
                DEFAULT_REALTIME_SUMMARY_STAT_BACK_TIMES);
    }

    /**
     * Start the etl service.
     */
    public void start() {
        clickhouseToMysql();
        mysqlToMysqlOfDay();
        mysqlToTenMinutesCache();
        mysqlToHalfHourCache();
        mysqlToHourCache();
    }

    /**
     * Aggregate data from mysql data source and store the aggregated data in the target mysql table.
     * The audit data cycle is days,and stored in table of day.
     */
    private void mysqlToMysqlOfDay() {
        DataQueue dataQueue = new DataQueue(queueSize);

        mysqlSourceOfTemp = new JdbcSource(dataQueue, buildMysqlSourceConfig(AuditCycle.DAY,
                Configuration.getInstance().get(KEY_DAILY_SUMMARY_STAT_BACK_TIMES,
                        DEFAULT_DAILY_SUMMARY_STAT_BACK_TIMES)));
        mysqlSourceOfTemp.start();

        SinkConfig sinkConfig = buildMysqlSinkConfig(Configuration.getInstance().get(KEY_MYSQL_SINK_INSERT_DAY_SQL,
                DEFAULT_MYSQL_SINK_INSERT_DAY_SQL));
        mysqlSinkOfDay = new JdbcSink(dataQueue, sinkConfig);
        mysqlSinkOfDay.start();
    }

    /**
     * Aggregate data from mysql data source and store in local cache for openapi.
     */
    private void mysqlToTenMinutesCache() {
        DataQueue dataQueue = new DataQueue(queueSize);
        mysqlSourceOfTenMinutesCache =
                new JdbcSource(dataQueue, buildMysqlSourceConfig(AuditCycle.MINUTE_10, statBackTimes));
        mysqlSourceOfTenMinutesCache.start();

        cacheSinkOfTenMinutesCache = new CacheSink(dataQueue, TenMinutesCache.getInstance().getCache());
        cacheSinkOfTenMinutesCache.start();
    }

    /**
     * Aggregate data from mysql data source and store in local cache for openapi.
     */
    private void mysqlToHalfHourCache() {
        DataQueue dataQueue = new DataQueue(queueSize);
        mysqlSourceOfHalfHourCache =
                new JdbcSource(dataQueue, buildMysqlSourceConfig(AuditCycle.MINUTE_30, statBackTimes));
        mysqlSourceOfHalfHourCache.start();

        cacheSinkOfHalfHourCache = new CacheSink(dataQueue, HalfHourCache.getInstance().getCache());
        cacheSinkOfHalfHourCache.start();
    }

    /**
     * Aggregate data from mysql data source and store in local cache for openapi.
     */
    private void mysqlToHourCache() {
        DataQueue dataQueue = new DataQueue(queueSize);
        mysqlSourceOfHourCache = new JdbcSource(dataQueue, buildMysqlSourceConfig(AuditCycle.HOUR, statBackTimes));
        mysqlSourceOfHourCache.start();

        cacheSinkOfHourCache = new CacheSink(dataQueue, HourCache.getInstance().getCache());
        cacheSinkOfHourCache.start();
    }

    /**
     * Aggregate data from clickhouse data source and store the aggregated data in the target mysql table.
     * The default audit data cycle is 5 minutes,and stored in a temporary table.
     */
    private void clickhouseToMysql() {
        DataQueue dataQueue = new DataQueue(queueSize);

        clickhouseSource = new JdbcSource(dataQueue, buildClickhouseSourceConfig());
        clickhouseSource.start();

        SinkConfig sinkConfig = buildMysqlSinkConfig(Configuration.getInstance().get(KEY_MYSQL_SINK_INSERT_TEMP_SQL,
                DEFAULT_MYSQL_SINK_INSERT_TEMP_SQL));
        mysqlSinkOfTemp = new JdbcSink(dataQueue, sinkConfig);
        mysqlSinkOfTemp.start();
    }

    /**
     * Build the configurations of mysql sink.
     *
     * @param insertSql
     * @return
     */
    private SinkConfig buildMysqlSinkConfig(String insertSql) {
        String driver = Configuration.getInstance().get(KEY_MYSQL_DRIVER, KEY_DEFAULT_MYSQL_DRIVER);
        String jdbcUrl = Configuration.getInstance().get(KEY_MYSQL_JDBC_URL);
        String userName = Configuration.getInstance().get(KEY_MYSQL_USERNAME);
        String passWord = Configuration.getInstance().get(KEY_MYSQL_PASSWORD);
        assert (Objects.nonNull(driver)
                && Objects.nonNull(jdbcUrl)
                && Objects.nonNull(userName)
                && Objects.nonNull(passWord));

        return new SinkConfig(
                insertSql,
                driver,
                jdbcUrl,
                userName,
                passWord);
    }

    /**
     * Build the configurations of mysql source.
     *
     * @return
     */
    private SourceConfig buildMysqlSourceConfig(AuditCycle auditCycle, int statBackTimes) {
        String driver = Configuration.getInstance().get(KEY_MYSQL_DRIVER, KEY_DEFAULT_MYSQL_DRIVER);
        String jdbcUrl = Configuration.getInstance().get(KEY_MYSQL_JDBC_URL);
        String userName = Configuration.getInstance().get(KEY_MYSQL_USERNAME);
        String passWord = Configuration.getInstance().get(KEY_MYSQL_PASSWORD);
        assert (Objects.nonNull(driver)
                && Objects.nonNull(jdbcUrl)
                && Objects.nonNull(userName));

        return new SourceConfig(auditCycle,
                Configuration.getInstance().get(KEY_MYSQL_SOURCE_QUERY_TEMP_SQL,
                        DEFAULT_MYSQL_SOURCE_QUERY_TEMP_SQL),
                statBackTimes,
                driver,
                jdbcUrl,
                userName,
                passWord);
    }

    /**
     * Build the configurations of clickhouse source.
     *
     * @return
     */
    private SourceConfig buildClickhouseSourceConfig() {
        String driver = Configuration.getInstance().get(KEY_CLICKHOUSE_DRIVER, DEFAULT_CLICKHOUSE_DRIVER);
        String jdbcUrl = Configuration.getInstance().get(KEY_CLICKHOUSE_JDBC_URL);
        String userName = Configuration.getInstance().get(KEY_CLICKHOUSE_USERNAME);
        String passWord = Configuration.getInstance().get(KEY_CLICKHOUSE_PASSWORD);
        assert (Objects.nonNull(driver)
                && Objects.nonNull(jdbcUrl)
                && Objects.nonNull(userName)
                && Objects.nonNull(passWord));

        return new SourceConfig(AuditCycle.MINUTE_5,
                Configuration.getInstance().get(KEY_CLICKHOUSE_SOURCE_QUERY_SQL,
                        DEFAULT_CLICKHOUSE_SOURCE_QUERY_SQL),
                Configuration.getInstance().get(KEY_REALTIME_SUMMARY_STAT_BACK_TIMES,
                        DEFAULT_REALTIME_SUMMARY_STAT_BACK_TIMES),
                driver,
                jdbcUrl,
                userName,
                passWord);
    }

    /**
     * Stop the etl service,and destroy related resources.
     */
    public void stop() {
        mysqlSourceOfTemp.destroy();
        mysqlSinkOfDay.destroy();

        clickhouseSource.destroy();
        mysqlSinkOfTemp.destroy();

        mysqlSourceOfTenMinutesCache.destroy();
        mysqlSourceOfHalfHourCache.destroy();
        mysqlSourceOfHourCache.destroy();

        cacheSinkOfTenMinutesCache.destroy();
        cacheSinkOfHalfHourCache.destroy();
        cacheSinkOfHourCache.destroy();
    }
}
