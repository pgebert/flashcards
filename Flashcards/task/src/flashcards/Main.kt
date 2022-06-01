package flashcards

import java.io.File
import java.io.FileNotFoundException
import kotlin.random.Random
import kotlin.random.nextInt

const val DELIMITER = "##"
val log = mutableListOf<String>()


/**
 * Command flow loop and entrypoint.
 *
 * @param args
 */
fun main(args: Array<String>) {

    val argsMap = args.toList().chunked(2).associate { it[0] to it[1] }

    var exit = false
    val stack = mutableListOf<Card>()

    init(argsMap, stack)

    while (!exit) {
        try {
            when (getCommand()) {
                Command.ADD -> addNewCard(stack)
                Command.REMOVE -> removeCard(stack)
                Command.IMPORT -> import(stack)
                Command.EXPORT -> export(stack)
                Command.ASK -> ask(stack)
                Command.EXIT -> exit = exit(argsMap, stack)
                Command.LOG -> saveLog(log)
                Command.HARDEST_CARD -> hardestCard(stack)
                Command.RESET_STATS -> resetStats(stack)
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }

}

/**
 * Init flashcards program by import saved flash cards.
 *
 * @param argsMap command line arguments
 * @param stack stack of flash cards
 */
fun init(argsMap: Map<String, String>, stack: MutableList<Card>) {

    if ("-import" in argsMap) {
        import(stack, fromFile = argsMap["-import"])
    }
}

/**
 * Custom println command to integrate logging.
 *
 * @param message message to print
 */
fun println(message: String) {
    log.add(message)
    kotlin.io.println(message)
}

/**
 * Custom readln command to integrate logging.
 *
 * @return read message from command line
 */
fun readln(): String {
    val message = kotlin.io.readln()
    log.add(message)
    return message
}

/**
 * Prompt the user for a command and return it.
 *
 * @return user command
 */
fun getCommand(): Command {
    println("Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):")

    while (true) {
        try {
            return Command.valueOf(readln().uppercase().replace(" ", "_"))
        } catch (e: IllegalArgumentException) {
            println("Please select an action from the list (add, remove, import, export, ask, exit, log, hardest card, reset stats):")
        }
    }

}

/**
 * Add new card to the stack
 *
 * @param stack stack of cards
 */
fun addNewCard(stack: MutableList<Card>) {

    println("The card")
    val term = readln()
    if (term in stack.map { it.term }) {
        throw Exception("The card \"$term\" already exists.")
    }

    println("The definition of the card::")
    val existingDefinitions = stack.map { it.definition }
    val definition = readln()

    if (definition in existingDefinitions) {
        throw Exception("The definition \"$definition\" already exists.")
    }

    val card = Card(term, definition)
    stack.add(card)

    println("The pair (\"${card.term}\":\"${card.definition}\") has been added.")
}

/**
 * Remove card from the stack
 *
 * @param stack stack of cards
 */
fun removeCard(stack: MutableList<Card>) {

    println("Which card?")
    val term = readln()

    if (term !in stack.map { it.term }) {
        throw Exception("Can't remove \"${term}\": there is no such card.")
    }

    stack.removeIf { it.term == term }
    println("The card has been removed.")

}

/**
 * Export the stack of cards.
 *
 * @param stack stack of cards
 * @param toFile output file
 */
fun export(stack: MutableList<Card>, toFile: String? = null) {

    var fileName = toFile

    if (fileName.isNullOrEmpty()) {
        println("File name:")
        fileName = readln()
    }

    File(fileName).writeText(stack.joinToString("\n") {
        it.term + DELIMITER + it.definition + DELIMITER + it.missed
    })
    println("${stack.size} cards have been saved.")

}

/**
 * Import and append a stack of cards to the existing stack
 *
 * @param stack stack of cards
 * @param fromFile input file
 */
fun import(stack: MutableList<Card>, fromFile: String? = null) {

    var fileName = fromFile

    if (fileName.isNullOrEmpty()) {
        println("File name:")
        fileName = readln()
    }

    try {
        val lines = File(fileName).readLines()

        lines.forEach { line ->
            val arguments = line.split(DELIMITER)

            assert(arguments.size == 3)

            val term = arguments[0]
            val definition = arguments[1]
            val missed = arguments[2].toInt()

            val card = Card(term, definition, missed)
            stack.add(card)
        }

        println("${lines.size} cards have been loaded.")

    } catch (e: FileNotFoundException) {
        throw Exception("File not found.")
    }


}

/**
 * Ask the definition of a card term multiple times to learn
 * the flashcard.
 *
 * @param stack stack of cards
 */
fun ask(stack: MutableList<Card>) {

    println("How many times to ask?")
    val times = readln().toInt()

    val hints = stack.associate { it.definition to it.term }

    repeat(times) {

        val randomIndex = Random.nextInt(0..stack.lastIndex)
        val card = stack[randomIndex]

        println("Print the definition of \"${card.term}\":")
        val answer = readln()


        if (!card.isCorrect(answer)) card.missed += 1

        val result = if (card.isCorrect(answer)) {
            "Correct!"
        } else if (answer in hints) {
            "Wrong. The right answer is \"${card.definition}\", but your definition is correct for \"${hints[answer]}\"."
        } else {
            "Wrong. The right answer is \"${card.definition}\"."
        }

        println(result)
    }

}

/**
 * Prints the hardest card with the most wrong answers.
 *
 * @param stack stack of cards
 */
fun hardestCard(stack: MutableList<Card>) {

    val maxMissed = stack.maxOfOrNull { it.missed } ?: 0
    val cardsMaxMissed = stack.filter { it.missed > 0 && it.missed == maxMissed }
    val cardsMaxMissedString = cardsMaxMissed.joinToString(", ") { "\"" + it.term + "\"" }

    val result = when {
        cardsMaxMissed.size > 1 -> "The hardest cards are $cardsMaxMissedString. You have $maxMissed errors answering them."
        cardsMaxMissed.size == 1 -> "The hardest card is $cardsMaxMissedString. You have $maxMissed errors answering it."
        else -> "There are no cards with errors."
    }

    println(result)
}

/**
 * Reset the statistics for the stack of cards.
 *
 * @param stack stack of cards.
 */
fun resetStats(stack: MutableList<Card>) {
    stack.forEach { it.missed = 0 }
    println("Card statistics have been reset.")
}

/**
 * Save log of the flashcard program.
 *
 * @param log log to save
 */
fun saveLog(log: MutableList<String>) {

    println("File name:")
    var fileName = readln()

    File(fileName).writeText(log.joinToString("\n"))
    println("The log has been saved.")
}

/**
 * Exit the program.
 *
 * @param argsMap command line arguments
 * @param stack stack of cards
 * @return success
 */
fun exit(argsMap: Map<String, String>, stack: MutableList<Card>): Boolean {

    if ("-export" in argsMap) {
        export(stack, toFile = argsMap["-export"])
    }

    println("Bye bye!")
    return true
}