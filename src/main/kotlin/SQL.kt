import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Debates : IntIdTable() {
    val name = varchar("name", 64).index()
    val msgStart = varchar("msg_start_id", 200).nullable()
    val msgEnd = varchar("msg_end_id", 200).nullable()
    val isFinished = bool("ended").default(false)
}

object Categories : IntIdTable() {
    val debate = reference("debate", Debates)
    val name = varchar("name", 64).index()
}

object Arguments : IntIdTable() {
    val person = varchar("person", 64)
    val category = reference("title", Categories)
    val text = varchar("text", 500).nullable()
    val messageId = varchar("message_id", 200).nullable()
}

// Entities

class Debate(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Debate>(Debates)

    var name by Debates.name
    var msgStart by Debates.msgStart
    var msgEnd by Debates.msgEnd
    var isFinished by Debates.isFinished
    val categories by Category referrersOn Categories.debate
}

class Category(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Category>(Categories)

    var debate by Debate referencedOn Categories.debate
    var name by Categories.name
    val arguments by Argument referrersOn Arguments.category
}

class Argument(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Argument>(Arguments)

    var person by Arguments.person
    var category by Category referencedOn Arguments.category
    var text by Arguments.text
    var messageId by Arguments.messageId
    val debate
        get() = category.debate
}
