import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import io.github.cdimascio.dotenv.dotenv
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.util.*

lateinit var jda: JDA

fun main(args: Array<String>) {
    Database.connect("jdbc:sqlite:data/data.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel =
            Connection.TRANSACTION_SERIALIZABLE

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Debates, Categories, Arguments)
    }

    val dotenv = dotenv()

    jda = JDABuilder
        .createDefault(dotenv["TOKEN"])
        .addEventListeners(Bot())
        .build()
}

class Bot : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
//        if (event.author )
        try {
            Main().subcommands(AWonderfulBot().subcommands(
                    Start(event),
                    End(event),
                    Modify(event),
                    ArgumentCommand(event),
                    List(event),
                    Display(event)
            )).parse(event.message.contentRaw.split(' '))
        }
        catch (e: Exception) {
            val keyword = dotenv()["KEYWORD"]
            if (event.message.contentRaw.contains(regex = "^$keyword".toRegex()) and ! event.author.isBot) {
                event.channel.sendMessage("\uD83E\uDD16 Bip boop, boop beep\n Are we speaking the same language ?").queue()
            }
        }
    }
}

class Main: CliktCommand() {
    override fun run() = Unit
}

class AWonderfulBot: CliktCommand(name = dotenv()["KEYWORD"]) {
    override fun run() = Unit
}

class Start(val event: MessageReceivedEvent): CliktCommand(help="Initializes a debate") {
    val name by argument()
    val msgStart : String? by argument(help = "Start of the message").optional()

    override fun run() {
        transaction {
            val debate = Debate.new {
                name = this@Start.name
                msgStart = this@Start.msgStart
            }
        }
        event.channel.sendMessage("Discussion $name Initialized").queue()
    }
}

class End(val event: MessageReceivedEvent): CliktCommand(help = "Ends a debate") {
    val name by argument()
    val msgEnd : String? by argument().optional()

    override fun run() {
        transaction {
            val debates = Debate.find {
                Debates.name eq name
            }
            assert(debates.count() == 1L)

            debates.first().apply {
                isFinished = true
                msgEnd = this@End.msgEnd
            }

        }
        event.message.channel.sendMessage("Discussion $name ended").queue()
    }
}

class ArgumentCommand(val event: MessageReceivedEvent): CliktCommand() {
    val category by argument()
    val message by argument()
    // reference ? -> lien ou livre etc

    override fun run() {

        val m = Message.JUMP_URL_PATTERN.matcher(message)
        val channelId = m.group("channel")
        val messageId = m.group("message")

        val channel = jda.getGuildChannelById(channelId)
        if(channel is MessageChannel) {
            val message = channel.retrieveMessageById(messageId).complete()

            transaction {
                val categories = Category.find {
                    Categories.name eq category
                }

                if(categories.count() > 0) {       //isn't it unuseful ?
                    val argument = Argument.new {
                        person = message.author.name
                        text = message.contentRaw
                        category = categories.first()
                    }
                }
            }
            event.message.delete().queue()
        }
    }
}

class Modify(val event: MessageReceivedEvent):CliktCommand() {
    val name by argument()
    val id by argument() //think about the way to call the argument
    val text by argument()

    override fun run() {
        TODO("Not yet implemented")
    }
}

class List(val event: MessageReceivedEvent):CliktCommand() {
    override fun run() {
        event.message.channel.sendTyping().queue()
        transaction {
            val allDebates = Debate.all().map { it.name }

            val myEmbed = MessageBuilder().append(allDebates.joinToString(separator = "\n") ).build()

            event.message.channel.sendMessage(myEmbed).queue()
        }
    }
}

class Display(val event: MessageReceivedEvent):CliktCommand() {
    val name by argument()

    override fun run() {
        TODO("Not yet implemented")
    }
}
