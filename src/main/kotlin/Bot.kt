import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.apache.logging.log4j.core.Logger
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

class Start: CliktCommand(help="Initializes a debate") {
    val name by argument()
    val msgStart : String? by argument(help = "Start of the message").optional()

    override fun run() {
        echo("Hi! Name is $name and start is $msgStart")
    }
}

class End: CliktCommand(help = "Ends a debate") {
    val name by argument()
    val msgStart : String? by argument().optional()

    override fun run() {

    }
}

class ArgumentCommand: CliktCommand() {
    val name by argument() // or id
    val person by argument()
    val title by argument()
    val text by argument()

    override fun run() {
        TODO("Not yet implemented")
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
    val id by argument()

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
        SchemaUtils.create(Debates, Arguments)



        val debate = Debate.new {
            name = "Test1"
        }


        val argument = Argument.new {
            person = "Alex"
            title = "Ceci est une courte description"
            this.debate = debate
        }

        for (debate in Debate.all()) {
            println("ici")
            println("${debate.name} ${debate.messageId}")
        }
    }

    Start().main(args)
}