package com.malachite.core.utilities

import scala.jdk.CollectionConverters.{MapHasAsScala, SetHasAsScala, ListHasAsScala, MapHasAsJava, SetHasAsJava, BufferHasAsJava}

object CollectionUtilities {

	def computeHashMapInitialCapacityFromExpectedElementCount(expectedElementCount: Int, loadFactor: Float = 0.75f): Int
		= (expectedElementCount / loadFactor).toInt + 1

	def createHashMap[K, V](expectedElementCount: Int = 16, loadFactor: Float = 0.75f): java.util.Map[K, V] = {
		val initialCapacity = computeHashMapInitialCapacityFromExpectedElementCount(expectedElementCount, loadFactor)

		new java.util.HashMap[K, V](initialCapacity)
	}

	def createConcurrentHashMap[K, V](expectedElementCount: Int = 16, loadFactor: Float = 0.75f): java.util.Map[K, V] = {
		val initialCapacity = computeHashMapInitialCapacityFromExpectedElementCount(expectedElementCount, loadFactor)

		new java.util.concurrent.ConcurrentHashMap[K, V](initialCapacity)
	}

	def toScalaMap[K, V](map: java.util.Map[K, V]): Map[K, V] = map.asScala.toMap

	def toJavaMap[K, V](map: Map[K, V]): java.util.Map[K, V] = map.asJava

	def createHashSet[T](expectedElementCount: Int = 16, loadFactor: Float = 0.75f): java.util.Set[T] = {
		val initialCapacity = computeHashMapInitialCapacityFromExpectedElementCount(expectedElementCount, loadFactor)

		new java.util.HashSet[T](initialCapacity)
	}

	def createConcurrentSet[T]: java.util.Set[T] = new java.util.concurrent.CopyOnWriteArraySet[T]

	def toScalaSet[T](set: java.util.Set[T]): Set[T] = set.asScala.toSet

	def toJavaSet[T](set: Set[T]): java.util.Set[T] = set.asJava

	def toScalaList[T](list: java.util.List[T]): List[T] = list.asScala.toList

	def toJavaList[T](list: List[T]): java.util.List[T] = list.toBuffer.asJava

	def removeElementAndCleanupIfEmpty[E, K, V <: java.util.Collection[E]](elementToRemove: E, key: K, map: java.util.Map[K, V]): Unit = {
		val nullCollection = null.asInstanceOf[V]

		map.compute(key, (_, collection) => {
			if (collection == null) { nullCollection }
			else {
				collection.remove(elementToRemove)

				if (collection.isEmpty) {
					nullCollection
				} else {
					collection
				}
			}
		})
	}
}
