import com.github.javafaker.Faker
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.*
import org.junit.Assert.assertEquals
import java.io.File
import java.sql.Connection
import java.util.*

const val DEBATES_COUNT = 5
const val CATEGORIES_BY_DEBATE = 5
const val ARGUMENTS_BY_CATEGORY = 5

class TestCommands {
    companion object {
        @JvmStatic @BeforeClass
        fun setUp() {
            Database.connect("jdbc:sqlite:data/test.db", "org.sqlite.JDBC")
            TransactionManager.manager.defaultIsolationLevel =
                Connection.TRANSACTION_SERIALIZABLE

            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(Debates, Categories, Arguments)

                val faker = Faker(Locale("fr"))

                val debates = List(DEBATES_COUNT) {
                    Debate.new {
                        name = faker.lorem().words(3).joinToString()
                    }
                }

                Debate.new {
                    name = "Test1"
                }

                val categories = List(CATEGORIES_BY_DEBATE * DEBATES_COUNT) {
                    Category.new {
                        name = faker.lorem().words(3).joinToString()
                        debate = debates[it / CATEGORIES_BY_DEBATE]
                    }
                }

                val arguments = List(
                    ARGUMENTS_BY_CATEGORY * CATEGORIES_BY_DEBATE * DEBATES_COUNT
                ) {
                    Argument.new {
                        person = faker.name().username()
                        category = categories[it / ARGUMENTS_BY_CATEGORY]
                        text = faker.lorem().paragraph(3)
                    }
                }

            }
        }

        @JvmStatic @AfterClass
        fun tearDown() {
            File("data/test.db").delete()
        }

    }



    @Test
    fun startCommandTest() {
        Start(null).main(arrayOf("MessageStart", "123456"))
        transaction {
            val debates = Debate.find {
                Debates.name eq "MessageStart"
            }
            assertEquals(1L, debates.count())
            assertEquals("123456", debates.first().msgStart)
            assertEquals(null, debates.first().msgEnd)
            assertEquals(false, debates.first().isFinished)
            assertEquals(0, debates.first().categories.count())
        }
    }

    @Test
    fun endCommandTest() {
        End(null).main(arrayOf("Test1", "789"))
        transaction {
            val debates = Debate.find {
                Debates.name eq "Test1"
            }
            assertEquals(1L, debates.count())
            assertEquals(null, debates.first().msgStart)
            assertEquals("789", debates.first().msgEnd)
            assertEquals(true, debates.first().isFinished)
            assertEquals(0, debates.first().categories.count())
        }
    }

}