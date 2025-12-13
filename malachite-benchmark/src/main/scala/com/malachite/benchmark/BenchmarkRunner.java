package com.malachite.benchmark;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkRunner {

	@Benchmark
	public long addTwoNumbers() {
		var a = 10L;
		var b = 20L;

		return a + b;
	}
}
