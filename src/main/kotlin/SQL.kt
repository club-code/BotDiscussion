import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Debates : IntIdTable() {
    val name = varchar("name", 64).index()
    val messageId = varchar("message_id", 18).nullable()
}

object Arguments : IntIdTable() {
    val debate = reference("debate", Debates)
    val person = varchar("person", 64)
    val title = varchar("title", 64).nullable()
    val messageId = varchar("message_id", 18).nullable()
}

class Debate(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Debate>(Debates)

    var name by Debates.name
    var messageId by Debates.messageId
    val arguments by Argument referrersOn Arguments.debate
}

class Argument(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Argument>(Arguments)

    var debate by Debate referencedOn Arguments.debate
    var person by Arguments.person
    var title by Arguments.title
    var messageId by Arguments.messageId
}
