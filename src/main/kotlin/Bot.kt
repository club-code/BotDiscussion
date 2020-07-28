import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

//fun main(args: Array<String>) {
//    JDABuilder
//        .createDefault(args[0])
//        .addEventListeners(Bot())
//        .build()
//}

class Bot : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {

    }
}

class Main: CliktCommand() {
    override fun run() = Unit
}

class Start: CliktCommand(help="Initializes a debate") {
    val name by argument()
    val msgStart : String? by argument(help = "Start of the message").optional()

    override fun run() {
        transaction {
            val debate = Debate.new {
                name = this@Start.name
                msgStart = this@Start.msgStart
            }
        }
    }
}

class End: CliktCommand(help = "Ends a debate") {
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
    }
}

class ArgumentCommand: CliktCommand() {
    val name by argument() // or id
    val person by argument()
    val category by argument()
    val text by argument()
    val message by argument()
    // reference ? -> lien ou livre etc

    override fun run() {
        transaction {
            val debates = Debate.find {
                Debates.name eq name
            }
            assert(debates.count() == 1L)

            if(debates.count() > 0) {
                val argument = Argument.new {
                    person = this@ArgumentCommand.person
                    text = this@ArgumentCommand.text
                }
            }
        }
    }
}

class Modify:CliktCommand() {
    val name by argument()
    val id by argument() //think about the way to call the argument
    val text by argument()

    override fun run() {
        TODO("Not yet implemented")
    }
}

class List:CliktCommand() {
    override fun run() {
        TODO("Not yet implemented")
    }
}

class Display:CliktCommand() {
    val name by argument()

    override fun run() {
        TODO("Not yet implemented")
    }
}

fun main(args:Array<String>) {
    Database.connect("jdbc:sqlite:data/data.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel =
        Connection.TRANSACTION_SERIALIZABLE

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Debates, Categories, Arguments)
    }

    Main().subcommands(
        Start(),
        End(),
        Modify(),
        ArgumentCommand(),
        List(),
        Display()
    ).main(args)
}