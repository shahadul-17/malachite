package com.malachite.core.threading

class EnhancedSemaphore(val permits: Int, val fair: Boolean = false)
    extends java.util.concurrent.Semaphore(permits, fair) {

    override def isFair: Boolean = fair
}
