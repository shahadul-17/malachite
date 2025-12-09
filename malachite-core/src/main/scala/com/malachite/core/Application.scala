package com.malachite.core

/**
 * All application classes shall implement
 * this trait.
 */
trait Application {

    /**
     * Performs any initialization operation (if needed).
     */
    def initialize(): Unit = { }

    /**
     * Performs any reset operation (if needed).
     * E.g., Resetting the entire database schema to the default state.
     */
    def reset(): Unit = { }

    /**
     * Executes service to perform necessary actions.
     */
    def execute(): Unit = { }

    /**
     * Releases resources.
     */
    def dispose(): Unit = { }
}
