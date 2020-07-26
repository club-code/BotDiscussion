import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.sql.Connection


class TestCommands {
    @Before
    fun setUp() {
        Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared", "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel =
            Connection.TRANSACTION_SERIALIZABLE

        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Debates, Arguments)

            val debate = Debate.new {
                name = "Test1"
            }

            val category = Category.new {
                name = "Ceci est une courte description"
                this.debate = debate
            }

            val argument = Argument.new {
                person = "Alex"
                this.category = category
                text = "Ceci est un normalement un long texte"
            }
        }
    }

    @After
    fun tearDown() {
    }

}