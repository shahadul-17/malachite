package com.malachite.benchmark

import com.malachite.core.Application
import com.malachite.core.utilities.SystemUtilities
import org.apache.logging.log4j.{Level, LogManager}
import org.openjdk.jmh.runner.options.TimeValue

import scala.compiletime.uninitialized

class BenchmarkApplication private extends Application {

    private val logger = LogManager.getLogger(getClass)
    private var options: org.openjdk.jmh.runner.options.Options = uninitialized
    private var jmhRunner: org.openjdk.jmh.runner.Runner = uninitialized

    override def initialize(): Unit = {
        logger.log(Level.INFO, "Initializing Benchmark application.")

        // when the application runs in debug mode, we shall not use forks...
        val forks = if (SystemUtilities.debugModeEnabled) { 0 } else { 1 }

        options = new org.openjdk.jmh.runner.options.OptionsBuilder()
            .include(s"${classOf[BenchmarkRunner].getPackage.getName}*")
            .forks(forks)
            .warmupIterations(1)
            .warmupTime(TimeValue.seconds(5))
            .mode(org.openjdk.jmh.annotations.Mode.Throughput)
            .mode(org.openjdk.jmh.annotations.Mode.AverageTime)
            .measurementTime(TimeValue.seconds(5))
            .measurementIterations(1)
            .build
        jmhRunner = new org.openjdk.jmh.runner.Runner(options)

        logger.log(Level.INFO, "Benchmark application initialization successful.")
    }

    override def execute(): Unit = {
        logger.log(Level.INFO, "Executing Benchmark application.")

        jmhRunner.run

        logger.log(Level.INFO, "Benchmark application execution completed.")
    }
}
