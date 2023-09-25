package no.nav.modia.soknadsstatus.kafka

interface DeadLetterMessageSkipService {
    suspend fun shouldSkip(key: String): Boolean
}

class DeadLetterMessageSkipServiceImpl(private val repository: DeadLetterMessageRepository) :
    DeadLetterMessageSkipService {
    override suspend fun shouldSkip(key: String): Boolean = repository.getAndMarkAsSkipped(key).isNotEmpty()
}
