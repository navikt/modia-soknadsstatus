package no.nav.modia.soknadsstatus.kafka

import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheus.PrometheusConfig
import java.util.concurrent.atomic.AtomicInteger

interface DeadLetterQueueMetricsGauge {
    fun increment()
    fun decrement()
    fun set(newValue: Int)
}

class DeadLetterQueueMetricsGaugeImpl(gaugeTitle: String, metricsRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(
    PrometheusConfig.DEFAULT)
) : DeadLetterQueueMetricsGauge {
    private val counter = AtomicInteger(0)

    private val gauge: Gauge = Gauge.builder(gaugeTitle) {
        counter.get()
    }.register(metricsRegistry)

    override fun increment() {
        counter.incrementAndGet()
    }

    override fun decrement() {
        counter.decrementAndGet()
    }

    override fun set(newValue: Int) {
        counter.set(newValue)
    }
}