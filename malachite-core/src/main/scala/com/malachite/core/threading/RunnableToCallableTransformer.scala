package com.malachite.core.threading

import java.util.concurrent.Callable

private class RunnableToCallableTransformer private (val runnable: Runnable)
	extends java.util.concurrent.Callable[Null] {

	override def call(): Null = {
		runnable.run()

		null
	}
}

object RunnableToCallableTransformer {
	def transform(runnable: Runnable): Callable[?]
		= new RunnableToCallableTransformer(runnable)
}
