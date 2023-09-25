package no.nav.modia.soknadsstatus

import kotlinx.coroutines.*
import no.nav.personoversikt.common.logging.Logging
import java.lang.Runnable
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

object BackgroundTask {
    fun launch(block: suspend CoroutineScope.() -> Unit): Job = GlobalScope.launch(Dispatchers.Unbounded) {
        try {
            block()
        } catch (e: Exception) {
            Logging.secureLog.error("Background task exited with: ", e)
        }
    }
}

/*
Use Dispatchers.Unbounded to allow unlimited number of coroutines to be dispatched. Without this
only a few will be allowed simultaneously (depending on the number of available cores) which may result
in cronjobs or Kafka-consumers not starting as intended.
*/
val Dispatchers.Unbounded get() = UnboundedDispatcher.unboundedDispatcher

class UnboundedDispatcher private constructor() : CoroutineDispatcher() {
    companion object {
        val unboundedDispatcher = UnboundedDispatcher()
    }

    private val threadPool = Executors.newCachedThreadPool()
    private val dispatcher = threadPool.asCoroutineDispatcher()
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatcher.dispatch(context, block)
    }
}
