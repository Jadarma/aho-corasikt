package io.github.jadarma.ahocorasikt.test

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.engine.concurrency.SpecExecutionMode

object TestConfig : AbstractProjectConfig() {
    override val specExecutionMode = SpecExecutionMode.Concurrent
    override val specExecutionOrder = SpecExecutionOrder.Lexicographic
}
