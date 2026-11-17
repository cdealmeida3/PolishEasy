package com.polisheasy.engine

import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * A conversation for the Chat Game, as `content/kpk1/chats/` writes it: a named [person] the
 * Learner talks to, in a [scene] that says who the Learner is and where, written in the [register]
 * the conversation should be held in. It is a small graph of [turns], from the first: in each the
 * person says a line and the Learner replies, and a right reply leads on to the turn it names.
 * It is offered once every one of its [items] is learned.
 */
@Serializable
data class ChatScript(
    val id: String,
    val person: String,
    val title: Localized,
    val scene: Localized,
    val register: Register? = null,
    val items: List<String> = emptyList(),
    val turns: List<ChatTurn> = emptyList(),
) {
    internal val turnsById: Map<String, ChatTurn> get() = turns.associateBy { it.id }
}

/**
 * One turn of a [ChatScript]: what the person [says] and its [meaning], and the [replies] the
 * Learner chooses from. A turn with no replies is the person's last line.
 */
@Serializable
data class ChatTurn(
    val id: String,
    val says: String,
    val meaning: Localized,
    val replies: List<ChatReply> = emptyList(),
)

/**
 * A reply the Learner can give. A [right] one leads on to the turn [next] names, or ends the
 * conversation when it names none; it is also accepted typed or said as any of its
 * [acceptedAnswers] (by default its [text]). A wrong one may be in the wrong [register], and its
 * [note] says gently why it does not fit, its Polish marked with `*` as in a Tip.
 */
@Serializable
data class ChatReply(
    val text: String,
    val meaning: Localized,
    val right: Boolean = false,
    val next: String? = null,
    val acceptedAnswers: List<String> = emptyList(),
    val register: Register? = null,
    val note: Localized? = null,
) {
    /** What is accepted typed or said: [acceptedAnswers], or the reply as written. */
    val answers: List<String> get() = acceptedAnswers.ifEmpty { listOf(text) }
}

/** One bubble of a chat: the person's line, or the Learner's ([mine]), [missed] when it is the right reply put in for a wrong one. */
data class ChatLine(val mine: Boolean, val text: String, val meaning: String, val missed: Boolean = false)

/** A reply to choose: its Polish, its meaning, and the register the content marks it with. */
data class ChatOption(val text: String, val meaning: String, val register: Register? = null)

/**
 * The Learner's reply to a turn: [text] as given, whether it was [correct] (typed with only an
 * accent missing, [accentWarning]), and the [reply] the chat goes on with, the right one when it
 * was wrong. [note] says why a wrong reply does not fit, its Polish marked, and [register] is the register it was in.
 */
data class ChatGiven(
    val text: String,
    val correct: Boolean,
    val reply: ChatOption,
    val accentWarning: Boolean = false,
    val note: MarkedText? = null,
    val register: Register? = null,
)

/** How the Learner replies in a Round of Chat. */
enum class ChatWay(val id: String) {
    /** Tap one of the replies. */
    CHOOSE("choose"),

    /** Type the reply. */
    TYPE("type"),

    /** Say the reply aloud, or choose it instead. */
    SAY("say"),
}

/**
 * A Round of Chat: one conversation with [person], replying the [way] chosen. [lines] are the
 * bubbles so far. Each reply is answered once; a wrong one shows the right one, which the
 * conversation goes on with. The result is the replies right first time.
 */
data class ChatRound(
    override val played: List<String>,
    val way: ChatWay,
    val person: String,
    val title: String,
    val scene: String,
    val register: Register?,
    val lines: List<ChatLine>,
    /** The reply being shown, until the Learner moves on. */
    val given: ChatGiven? = null,
    /** In the say way, whether this turn's replies are shown to choose from instead. */
    val choosing: Boolean = false,
    /** Whether each reply so far was right first time. */
    val results: List<Boolean> = emptyList(),
    internal val script: ChatScript,
    internal val language: String,
    internal val turnId: String?,
    internal val seed: Int,
) : Round {
    override val game: Game get() = Game.CHAT
    override val done: Int get() = results.size
    override val total: Int get() = done + toCome(turn)

    /** Finished once the conversation has nothing more to reply to: the person said their last line, or the last reply ended it. */
    override val finished: Boolean get() = given == null && (turn?.replies.isNullOrEmpty())

    /** A chat is one screen that grows: nothing is replaced. */
    override val step: Int get() = 0
    override val score: Score get() = Score(results.count { it }, of = done)
    override val asked: List<String> get() = listOf(script.id)

    private val turn: ChatTurn? get() = turnId?.let(script.turnsById::get)

    /** The replies of the turn being shown, mixed, or none once the chat is finished. */
    val options: List<ChatOption>
        get() = mixedReplies().map { ChatOption(it.text, localize(it.meaning), it.register) }

    /** Whether the Learner is to reply now: a turn is waiting and no reply has been given. */
    val replying: Boolean get() = given == null && !finished

    /** Replies with the option at [option]. No change if there is none there or a reply was given. */
    fun choose(option: Int): ChatRound {
        if (!replying) return this
        val reply = mixedReplies().getOrNull(option) ?: return this
        return answered(reply, reply.text, reply.right, accentWarning = false)
    }

    /**
     * Replies with [typed], checked as a typed answer is: against every right reply's Accepted
     * Answers, then against the wrong replies, whose note it gets.
     */
    fun type(typed: String): ChatRound {
        val turn = turn ?: return this
        if (!replying || typed.isBlank()) return this
        turn.replies.filter { it.right }.forEach { reply ->
            val feedback = checkTyped(typed, reply.answers)
            if (feedback.correct) return answered(reply, typed.trim(), correct = true, accentWarning = feedback.accentWarning)
        }
        turn.replies.filter { !it.right }.firstOrNull { checkTyped(typed, listOf(it.text)).correct }
            ?.let { return answered(it, typed.trim(), correct = false, accentWarning = false) }
        return answered(null, typed.trim(), correct = false, accentWarning = false)
    }

    /**
     * Replies with what a speech recognizer [heard], best first, matched leniently ([saidAs]).
     * No change when it heard nothing: the Learner can try again or choose instead.
     */
    fun say(heard: List<String>): ChatRound {
        val turn = turn ?: return this
        val said = heard.filter { it.isNotBlank() }
        if (!replying || said.isEmpty()) return this
        turn.replies.filter { it.right }.firstOrNull { saidAs(said, it.answers) != null }
            ?.let { return answered(it, said.first(), correct = true, accentWarning = false) }
        turn.replies.filter { !it.right }.firstOrNull { saidAs(said, listOf(it.text)) != null }
            ?.let { return answered(it, said.first(), correct = false, accentWarning = false) }
        return answered(null, said.first(), correct = false, accentWarning = false)
    }

    /** In the say way, shows this turn's replies to choose from: speaking never blocks. */
    fun chooseInstead(): ChatRound = if (way == ChatWay.SAY && replying) copy(choosing = true) else this

    /**
     * Moves on from a reply: it joins the chat (the right one in place of a wrong one), and the
     * person says the line it leads to. No change while no reply has been given.
     */
    fun next(): ChatRound {
        val given = given ?: return this
        val taken = turn?.replies?.firstOrNull { it.text == given.reply.text && it.right } ?: return this
        val after = taken.next?.let(script.turnsById::get)
        val mine = ChatLine(mine = true, text = taken.text, meaning = localize(taken.meaning), missed = !given.correct)
        return copy(
            lines = lines + mine + listOfNotNull(after?.let { ChatLine(mine = false, it.says, localize(it.meaning)) }),
            given = null,
            choosing = false,
            turnId = after?.id,
        )
    }

    /** [reply] given as [text]: right, or wrong with the first right reply to go on with. */
    private fun answered(reply: ChatReply?, text: String, correct: Boolean, accentWarning: Boolean): ChatRound {
        val turn = checkNotNull(turn)
        val onWith = if (correct) checkNotNull(reply) else turn.replies.first { it.right }
        return copy(
            given = ChatGiven(
                text = text,
                correct = correct,
                reply = ChatOption(onWith.text, localize(onWith.meaning), onWith.register),
                accentWarning = accentWarning,
                note = reply?.takeIf { !correct }?.note?.let(::localize)?.let(::readPolishMarks),
                register = reply?.takeIf { !correct }?.register,
            ),
            results = results + correct,
        )
    }

    /** The current turn's replies in an order that depends only on the Round and the turn, never as authored where it can help it. */
    private fun mixedReplies(): List<ChatReply> {
        val replies = turn?.replies.orEmpty()
        if (replies.size < 2) return replies
        val mixed = replies.shuffled(Random(seed + done))
        return if (mixed == replies) replies.drop(1) + replies.first() else mixed
    }

    private fun localize(text: Localized): String = text[language] ?: text.values.firstOrNull().orEmpty()

    /** The replies still to give from [turn], following the first right reply of each. */
    private fun toCome(turn: ChatTurn?): Int {
        var count = 0
        var at = turn
        val seen = mutableSetOf<String>()
        while (at != null && at.replies.isNotEmpty() && seen.add(at.id)) {
            if (at.id != turnId || given == null) count++
            at = at.replies.firstOrNull { it.right }?.next?.let(script.turnsById::get)
        }
        return count
    }
}

internal object ChatRules : GameRules {
    const val CHAT = "chat"
    const val WAY = "way"

    override val game = Game.CHAT

    /** The conversations whose every Item is learned, in the content's order. */
    private fun scripts(learned: Learned): List<ChatScript> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.chats.filter { script -> script.turns.isNotEmpty() && known.containsAll(script.items) }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (scripts(learned).isEmpty()) GameState.NEEDS_MORE else GameState.READY

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> =
        choices(learned, wanted, sound, speech = false)

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean, speech: Boolean): List<ChoiceView> {
        val chats = scripts(learned).map { OptionView(it.id, learned.localize(it.title)) }
        val ways = ChatWay.entries.filter { it != ChatWay.SAY || speech }.map { OptionView(it.id) }
        return listOf(choice(CHAT, chats, wanted.getOrNull(0)), choice(WAY, ways, wanted.getOrNull(1)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round =
        start(learned, selected, sound, speech = false, random, seen, avoidFirst)

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, speech: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val script = scripts(learned).first { it.id == selected[0] }
        val first = script.turns.first()
        val language = learned.language
        return ChatRound(
            played = selected,
            way = ChatWay.entries.firstOrNull { it.id == selected[1] } ?: ChatWay.CHOOSE,
            person = script.person,
            title = learned.localize(script.title),
            scene = learned.localize(script.scene),
            register = script.register,
            lines = listOf(ChatLine(mine = false, first.says, first.meaning[language].orEmpty())),
            script = script,
            language = language,
            turnId = first.id,
            seed = random.nextInt(),
        )
    }
}
