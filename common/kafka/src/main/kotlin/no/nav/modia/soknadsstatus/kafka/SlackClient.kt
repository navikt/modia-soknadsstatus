package no.nav.modia.soknadsstatus.kafka

import com.slack.api.Slack
import com.slack.api.webhook.WebhookResponse
import org.slf4j.LoggerFactory

class SlackClient(private val webhookUrl: String) {
    private val log = LoggerFactory.getLogger(SlackClient::class.java)
    private var slack = Slack.getInstance()

    fun postMessage(text: String): WebhookResponse {
        val payload = "{\"text\": \"$text\"}"
        log.info("Sender $text til slack")
        return slack.send(webhookUrl, payload)
    }
}
