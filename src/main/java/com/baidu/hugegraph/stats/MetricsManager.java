/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baidu.hugegraph.stats;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ganglia.GangliaReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import info.ganglia.gmetric4j.gmetric.GMetric;
import info.ganglia.gmetric4j.gmetric.GMetric.UDPAddressingMode;

/**
 * Singleton that contains and configures HugeGraph's {@code MetricRegistry}.
 */
public class MetricsManager {

    private static final MetricsManager INSTANCE = new MetricsManager();

    private static final Logger logger =
            LoggerFactory.getLogger(MetricsManager.class);

    private final MetricRegistry registry;
    private ConsoleReporter consoleReporter = null;
    private CsvReporter csvReporter = null;
    private JmxReporter jmxReporter = null;
    private Slf4jReporter slf4jReporter = null;
    private GangliaReporter gangliaReporter = null;
    private GraphiteReporter graphiteReporter = null;

    /**
     * Private constructor for singleton.
     */
    private MetricsManager() {
        registry = new MetricRegistry();
    }

    /**
     * Return the Singleton Metrics Manager.
     *
     * @return the single {@code MetricManager}
     */
    public static MetricsManager instance() {
        return INSTANCE;
    }

    /**
     * Return the HugeGraph Metrics registry.
     *
     * @return the single {@code MetricRegistry} used for all of HugeGraph's
     *         Metrics monitoring
     */
    public MetricRegistry getRegistry() {
        return registry;
    }

    /**
     * Create a {@link ConsoleReporter} attached to the HugeGraph Metrics
     * registry.
     *
     * @param reportInterval
     *            time to wait between dumping metrics to the console
     */
    public synchronized void addConsoleReporter(Duration reportInterval) {
        if (consoleReporter != null) {
            logger.debug("Metrics ConsoleReporter already active; " +
                         "not creating another");
            return;
        }

        consoleReporter = ConsoleReporter.forRegistry(getRegistry()).build();
        consoleReporter.start(reportInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Stop a {@link ConsoleReporter} previously created by a call to
     * {@link #addConsoleReporter(Duration)} and release it for GC. Idempotent
     * between calls to the associated add method. Does nothing before the first
     * call to the associated add method.
     */
    public synchronized void removeConsoleReporter() {
        if (consoleReporter != null) {
            consoleReporter.stop();
        }
        consoleReporter = null;
    }

    /**
     * Create a {@link CsvReporter} attached to the HugeGraph Metrics registry.
     * <p>
     * The {@code output} argument must be non-null but need not exist. If it
     * doesn't already exist, this method attempts to create it by calling
     * {@link File#mkdirs()}.
     *
     * @param reportInterval
     *            time to wait between dumping metrics to CSV files in
     *            the configured directory
     * @param output
     *            the path to a directory into which Metrics will periodically
     *            write CSV data
     */
    public synchronized void addCsvReporter(Duration reportInterval,
                                            String output) {

        File outputDir = new File(output);

        if (csvReporter != null) {
            logger.debug("Metrics CsvReporter already active; " +
                         "not creating another");
            return;
        }

        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                logger.warn("Failed to create CSV metrics dir {}", outputDir);
            }
        }

        csvReporter = CsvReporter.forRegistry(getRegistry()).build(outputDir);
        csvReporter.start(reportInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Stop a {@link CsvReporter} previously created by a call to
     * {@link #addCsvReporter(Duration, String)} and release it for GC.
     * Idempotent between calls to the associated add method. Does nothing
     * before the first call to the associated add method.
     */
    public synchronized void removeCsvReporter() {
        if (csvReporter != null) {
            csvReporter.stop();
        }
        csvReporter = null;
    }

    /**
     * Create a {@link JmxReporter} attached to the HugeGraph Metrics registry.
     * <p>
     * If {@code domain} or {@code agentId} is null, then Metrics's uses its own
     * internal default value(s).
     * <p>
     * If {@code agentId} is non-null, then
     * {@link MBeanServerFactory#findMBeanServer(String)} must return exactly
     * one {@code MBeanServer}. The reporter will register with that server. If
     * the {@code findMBeanServer(agentId)} call returns no or multiple servers,
     * then this method logs an error and falls back on the Metrics default for
     * {@code agentId}.
     *
     * @param domain
     *            the JMX domain in which to continuously expose metrics
     * @param agentId
     *            the JMX agent ID
     */
    public synchronized void addJmxReporter(String domain, String agentId) {
        if (jmxReporter != null) {
            logger.debug("Metrics JmxReporter already active; " +
                         "not creating another");
            return;
        }

        JmxReporter.Builder builder = JmxReporter.forRegistry(getRegistry());

        if (domain != null) {
            builder.inDomain(domain);
        }

        if (agentId != null) {
            List<MBeanServer> servs =
                    MBeanServerFactory.findMBeanServer(agentId);
            if (servs != null && servs.size() == 1) {
                builder.registerWith(servs.get(0));
            } else {
                logger.error("Metrics Slf4jReporter agentId {} does not " +
                             "resolve to a single MBeanServer", agentId);
            }
        }

        jmxReporter = builder.build();
        jmxReporter.start();
    }

    /**
     * Stop a {@link JmxReporter} previously created by a call to
     * {@link #addJmxReporter(String, String)} and release it for GC. Idempotent
     * between calls to the associated add method. Does nothing before the first
     * call to the associated add method.
     */
    public synchronized void removeJmxReporter() {
        if (jmxReporter != null) {
            jmxReporter.stop();
        }
        jmxReporter = null;
    }

    /**
     * Create a {@link Slf4jReporter} attached to the HugeGraph Metrics registry
     * <p>
     * If {@code loggerName} is null, or if it is non-null but
     * {@link LoggerFactory#getLogger(String)} returns null, then Metrics's
     * default Slf4j logger name is used instead.
     *
     * @param reportInterval
     *            time to wait between writing metrics to the Slf4j
     *            logger
     * @param loggerName
     *            the name of the Slf4j logger that receives metrics
     */
    public synchronized void addSlf4jReporter(Duration reportInterval,
                                              String loggerName) {
        if (slf4jReporter != null) {
            logger.debug("Metrics Slf4jReporter already active; " +
                         "not creating another");
            return;
        }

        Slf4jReporter.Builder builder =
                Slf4jReporter.forRegistry(getRegistry());

        if (loggerName != null) {
            Logger slfLogger = LoggerFactory.getLogger(loggerName);
            if (slfLogger != null) {
                builder.outputTo(slfLogger);
            } else {
                logger.error("Logger with name {} could not be obtained",
                             loggerName);
            }
        }

        slf4jReporter = builder.build();
        slf4jReporter.start(reportInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Stop a {@link Slf4jReporter} previously created by a call to
     * {@link #addSlf4jReporter(Duration, String)} and release it for GC.
     * Idempotent between calls to the associated add method. Does nothing
     * before the first call to the associated add method.
     */
    public synchronized void removeSlf4jReporter() {
        if (slf4jReporter != null) {
            slf4jReporter.stop();
        }
        slf4jReporter = null;
    }

    /**
     * Create a {@link GangliaReporter} attached to the HugeGraph Metrics
     * registry.
     * <p>
     * {@code groupOrHost} and {@code addressingMode} must be non-null. The
     * remaining non-primitive arguments may be null. If {@code protocol31} is
     * null, then true is assumed. Null values of {@code hostUUID} or
     * {@code spoof} are passed into the {@link GMetric} constructor, which
     * causes Ganglia to use its internal logic for generating a default UUID
     * and default reporting hostname (respectively).
     *
     * @param groupOrHost
     *            the multicast group or unicast hostname to which Ganglia
     *            events are sent
     * @param port
     *            the port to which events are sent
     * @param addressingMode
     *            whether to send events with multicast or unicast
     * @param ttl
     *            multicast ttl (ignored for unicast)
     * @param protocol31
     *            true to use Ganglia protocol version 3.1, false to use 3.0
     * @param hostUUID
     *            uuid for the host
     * @param spoof
     *            override this machine's IP/hostname as it appears on the
     *            Ganglia server
     * @param reportInterval
     *            titme to wait before sending data to the ganglia
     *            unicast host or multicast group
     * @throws IOException
     *             when a {@link GMetric} can't be instantiated using the
     *             provided arguments
     */
    public synchronized void addGangliaReporter(
            String groupOrHost, int port, UDPAddressingMode addressingMode,
            int ttl, Boolean protocol31, UUID hostUUID, String spoof,
            Duration reportInterval) throws IOException {

        Preconditions.checkNotNull(groupOrHost);
        Preconditions.checkNotNull(addressingMode);

        if (gangliaReporter != null) {
            logger.debug("Metrics GangliaReporter already active;" +
                         "not creating another");
            return;
        }

        if (protocol31 == null) {
            protocol31 = true;
        }
        GMetric ganglia = new GMetric(groupOrHost, port, addressingMode, ttl,
                                      protocol31, hostUUID, spoof);

        GangliaReporter.Builder builder =
                GangliaReporter.forRegistry(getRegistry());

        gangliaReporter = builder.build(ganglia);
        gangliaReporter.start(reportInterval.toMillis(), TimeUnit.MILLISECONDS);

        logger.info("Configured Ganglia Metrics reporter host={} interval={} " +
                    "port={} addrmode={} ttl={} proto31={} uuid={} spoof={}",
                    groupOrHost, reportInterval, port, addressingMode, ttl,
                    protocol31, hostUUID, spoof);
    }

    /**
     * Stop a {@link GangliaReporter} previously created by a call to
     * {@link #addGangliaReporter(String, int, UDPAddressingMode, int, Boolean,
     * UUID, String, Duration)}
     * and release it for GC. Idempotent between calls to the associated add
     * method. Does nothing before the first call to the associated add method.
     */
    public synchronized void removeGangliaReporter() {
        if (gangliaReporter != null) {
            gangliaReporter.stop();
        }
        gangliaReporter = null;
    }

    /**
     * Create a {@link GraphiteReporter} attached to the HugeGraph Metrics
     * registry.
     * <p>
     * If {@code prefix} is null, then Metrics's internal default prefix is used
     * (empty string at the time this comment was written).
     *
     * @param host
     *            the host to which Graphite reports are sent
     * @param port
     *            the port to which Graphite reports are sent
     * @param prefix
     *            the optional metrics prefix
     * @param reportInterval
     *            time to wait between sending metrics to the configured
     *            Graphite host and port
     */
    public synchronized void addGraphiteReporter(String host, int port,
                                                 String prefix,
                                                 Duration reportInterval) {

        Preconditions.checkNotNull(host);

        Graphite graphite = new Graphite(new InetSocketAddress(host, port));

        GraphiteReporter.Builder builder =
                GraphiteReporter.forRegistry(getRegistry());

        if (prefix != null) {
            builder.prefixedWith(prefix);
        }
        builder.filter(MetricFilter.ALL);

        graphiteReporter = builder.build(graphite);
        graphiteReporter.start(reportInterval.toMillis(),
                               TimeUnit.MILLISECONDS);
        logger.info("Configured Graphite reporter host={} interval={} " +
                    "port={} prefix={}", host, reportInterval, port, prefix);
    }

    /**
     * Stop a {@link GraphiteReporter} previously created by a call to
     * {@link #addGraphiteReporter(String, int, String, Duration)} and release
     * it for GC. Idempotent between calls to the associated add method. Does
     * nothing before the first call to the associated add method.
     */
    public synchronized void removeGraphiteReporter() {
        if (graphiteReporter != null) {
            graphiteReporter.stop();
        }
        graphiteReporter = null;
    }

    /**
     * Remove all HugeGraph Metrics reporters previously configured through the
     * {@code add*} methods on this class.
     */
    public synchronized void removeAllReporters() {
        removeConsoleReporter();
        removeCsvReporter();
        removeJmxReporter();
        removeSlf4jReporter();
        removeGangliaReporter();
        removeGraphiteReporter();
    }

    public Counter getCounter(String name) {
        return getRegistry().counter(name);
    }

    public Counter getCounter(String prefix, String... names) {
        return getRegistry().counter(MetricRegistry.name(prefix, names));
    }

    public Timer getTimer(String name) {
        return getRegistry().timer(name);
    }

    public Timer getTimer(String prefix, String... names) {
        return getRegistry().timer(MetricRegistry.name(prefix, names));
    }

    public Histogram getHistogram(String name) {
        return getRegistry().histogram(name);
    }

    public Histogram getHistogram(String prefix, String... names) {
        return getRegistry().histogram(MetricRegistry.name(prefix, names));
    }

    public boolean remove(String name) {
        return getRegistry().remove(name);
    }
}


