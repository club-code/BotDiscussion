import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

fun main(args: Array<String>) {
    JDABuilder
        .createDefault(args[0])
        .addEventListeners(Bot())
        .build()
}

class Bot : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {

    }
}