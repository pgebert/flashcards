package flashcards

/**
 * Card represents a flash card.
 *
 * @property term
 * @property definition
 * @property missed
 * @constructor Create empty Card
 */
data class Card(
    val term: String,
    val definition: String,
    var missed: Int = 0
) {
    fun isCorrect(answer: String) = answer == definition
}