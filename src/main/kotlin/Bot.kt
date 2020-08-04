import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import io.github.cdimascio.dotenv.dotenv
import net.dv8tion.jda.api.EmbedBuilder
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
import java.awt.Color
import java.sql.Connection

lateinit var jda: JDA

fun main(args: Array<String>) {
    Database.connect("jdbc:sqlite:data/data.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel =
        Connection.TRANSACTION_SERIALIZABLE

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Debates, Categories, Arguments)
    }

    jda = JDABuilder
        .createDefault(dotenv()["TOKEN"])
        .addEventListeners(Bot())
        .build()
}

private val commandPrefix = dotenv()["KEYWORD"]

class Bot : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
//        if (event.author )
        var errorMessage: String? = null
        val arguments = event.message.contentRaw.trim().split(' ')
        if (!event.author.isBot && arguments[0] == commandPrefix) {
            try {
                Main().subcommands(
                    AWonderfulBot().subcommands(
                        Start(event),
                        End(event),
                        Modify(event),
                        CategoryCommand(event),
                        ArgumentCommand(event),
                        List(event),
                        Display(event)
                    )
                ).parse(arguments)
            } catch (e: PrintHelpMessage) {
                errorMessage = e.command.getFormattedHelp()
            } catch (e: PrintMessage) {
                errorMessage = e.message
            } catch (e: UsageError) {
                errorMessage = e.helpMessage()
            } catch (e: CliktError) {
                errorMessage = e.message
            } catch (e: Abort) {
                println("Aborted!")
            } finally {
                if (errorMessage != null) {
                    event.channel.sendMessage(errorMessage).queue()
                }
            }
        }

    }
}

class Main : CliktCommand(name = "") {
    override fun run() = Unit
}

class AWonderfulBot : CliktCommand(name = commandPrefix) {
    override fun run() = Unit
}

class Start(val event: MessageReceivedEvent?) : CliktCommand(help = "Initializes a debate") {
    val name by argument(help = "Debate name")
    val msgStart: String? by argument(help = "Starting message url").optional()

    override fun run() {
        transaction {
            val debate = Debate.new {
                name = this@Start.name
                msgStart = this@Start.msgStart
            }
        }
        event?.channel?.sendMessage("Discussion $name Initialized")?.queue()
    }
}

class End(val event: MessageReceivedEvent?) : CliktCommand(help = "Ends a debate") {
    val name by argument(help = "Name of the debate")
    val msgEnd: String? by argument(help = "Ending message").optional()

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
        event?.message?.channel?.sendMessage("Discussion $name ended")?.queue()
    }
}

class CategoryCommand(val event: MessageReceivedEvent) : CliktCommand(name = "category") {
    val debate by argument()
    val name by argument()

    override fun run() {
        event.channel.sendTyping().queue()
        transaction {
            val debates = Debate.find {
                Debates.name eq debate
            }

            if (debates.count() > 0) {
                val category = Category.new {
                    debate = debates.first()
                    name = this@CategoryCommand.name
                }
            }
        }
        event.channel.sendMessage("Category $name for $debate successfully created").queue()
    }
}

class ArgumentCommand(val event: MessageReceivedEvent) : CliktCommand(name = "argument") {
    val category by argument()
    val message by argument()
    // reference ? -> lien ou livre etc

    override fun run() {
        val m = Message.JUMP_URL_PATTERN.matcher(message)
        m.find()
        val channelId = m.group("channel")
        val messageId = m.group("message")

        val channel = jda.getGuildChannelById(channelId)
        if (channel is MessageChannel) {
            val message = channel.retrieveMessageById(messageId).complete()

            transaction {
                val categories = Category.find {
                    Categories.name eq category
                }

                if (categories.count() > 0) {       //isn't it unuseful ?
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

class Modify(val event: MessageReceivedEvent) : CliktCommand() {
    val name by argument()
    val id by argument() //think about the way to call the argument
    val text by argument()

    override fun run() {
        TODO("Not yet implemented")
    }
}

class List(val event: MessageReceivedEvent) : CliktCommand() {
    override fun run() {
        event.message.channel.sendTyping().queue()
        val allDebates = transaction {
            Debate.all().map { "**" + it.id.toString() + "**   " + it.name }
        }

        val myEmbed = EmbedBuilder()
                .addField("", allDebates.joinToString(separator = "\n"), false)
                .setColor(Color.MAGENTA)
                .setTitle("Discussions")
                .setDescription("Every discussion stored")
                .build()

        event.message.channel.sendMessage(myEmbed).queue()
    }
}

class Display(val event: MessageReceivedEvent) : CliktCommand() {
    val name by argument()

    override fun run() {
        transaction {
            val debates = Debate.find {
                Debates.name eq name
            }
            val debate = debates.first()
            event.message.channel.sendMessage(
                EmbedBuilder()
                    .setTitle(debate.name)
                    .addField("Categories", debate.categories.map { "**" + it.id + "**   " + it.name }.joinToString(separator = "/n" ), false)
                    .setColor(Color.ORANGE)
                    .build()
            ).queue()
        }
    }
}
