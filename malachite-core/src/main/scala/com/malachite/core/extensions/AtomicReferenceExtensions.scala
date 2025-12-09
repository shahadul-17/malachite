package com.malachite.core.extensions

object AtomicReferenceExtensions {
	extension [T] (atomicReference: java.util.concurrent.atomic.AtomicReference[T]) {

		private def tryInverseCompareAndSet(currentValue: T, newValue: T): Option[Boolean] = {
			if (currentValue == newValue) { Some(false) }
			else if (atomicReference.compareAndSet(currentValue, newValue)) { Some(true) }
			else { None }
		}

		def inverseCompareAndSet(newValue: T): Boolean = {
			var updated = false
			var done = false

			while (!done) {
				val currentValue = atomicReference.get

				tryInverseCompareAndSet(currentValue, newValue) match {
					case Some(result) =>
						updated = result
						done = true
					case _ =>
						// shall retry...
				}
			}

			updated
		}

		def getAndInverseCompareAndSet(newValue: T): T = {
			var previousValue = null.asInstanceOf[T]
			var done = false

			while (!done) {
				val currentValue = atomicReference.get

				tryInverseCompareAndSet(currentValue, newValue) match {
					case Some(_) =>
						previousValue = currentValue
						done = true
					case _ =>
						// shall retry...
				}
			}

			previousValue
		}
	}
}
