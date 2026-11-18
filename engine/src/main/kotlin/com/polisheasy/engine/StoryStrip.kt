package com.polisheasy.engine

import kotlin.random.Random

/**
 * One picture of a Story strip story: the sentence it shows, [text] (*Potem umyłam zęby.*), what it
 * means, the Item whose Picture it is, and the [items] the sentence uses, which must be learned for
 * the story to be played.
 */
data class StoryPanel(val text: String, val meaning: Localized, val picture: String, val items: List<String>)

/** A story of Story strip, told in the past: its [title] and its [panels] in the order they happen. */
data class Story(val title: Localized, val panels: List<StoryPanel>) {
    /** The whole story, its panels' sentences one after another, said or written for the Learner to follow. */
    val text: String get() = panels.joinToString(" ") { it.text }
}

/** A picture of the story being played: its [picture], and the sentence it shows with what it means, given once it is placed. */
data class StoryTile(val picture: Picture, val text: String, val meaning: String)

/**
 * A story of a Round: its [title], its [text], its [tiles] in the mixed order they are shown, and
 * [order], the tile of each place of the strip, first to last.
 */
data class StoryView(val key: String, val title: String, val text: String, val tiles: List<StoryTile>, val order: List<Int>)

/** One question of Story strip: which tile goes in [place] of the strip of the round's [story]: [tile]. */
data class StoryQuestion(val story: Int, val place: Int, val tile: Int)

/**
 * A Round of Story strip, chapter 13's Game. A short story in the past is said, or with [listen]
 * off written: *Najpierw wstałam o siódmej. Potem umyłam zęby.* Its pictures are mixed up, and the
 * Learner taps them in the order the story tells them, each put in the strip in turn. A wrong
 * picture is marked and the right one put in its place. The result is the pictures put in right
 * first try.
 */
data class StoryRound(
    override val played: List<String>,
    val listen: Boolean,
    val stories: List<StoryView>,
    val run: Run<StoryQuestion>,
) : Round {
    override val game: Game get() = Game.STORY_STRIP
    override val done: Int get() = run.results.size
    override val total: Int get() = run.questions.size
    override val finished: Boolean get() = run.finished

    /** Each story is a step of its own, which stays while its strip is filled. */
    override val step: Int get() = run.current?.story ?: stories.size
    override val score: Score get() = Score(run.firstTry, of = total)
    override val asked: List<String> get() = stories.map { it.key }

    /** The place being asked, or null once the Round is finished. */
    val question: StoryQuestion? get() = run.current

    /** The story being played, or null once the Round is finished. */
    val story: StoryView? get() = question?.let { stories[it.story] }

    /** The answer to the place being asked, until the Learner moves on. */
    val given: Given? get() = run.given

    /** The tiles in the strip of the story being played, first to last: the places asked before, and the one being asked once answered. */
    val placed: List<Int>
        get() {
            val now = question ?: return emptyList()
            val story = stories[now.story]
            return story.order.take(now.place + if (given != null) 1 else 0)
        }

    /** The tile chosen for the place being asked, until the Learner moves on. */
    val chosen: Int? get() = given?.text?.toIntOrNull()

    /** Taps the tile at [index] as the next picture. No change for no such tile, one already in the strip, or once answered. */
    fun choose(index: Int): StoryRound {
        val question = question ?: return this
        val story = stories[question.story]
        if (given != null || index !in story.tiles.indices || index in placed) return this
        return copy(run = run.answer(index.toString(), index == question.tile))
    }

    /** Moves on from an answered place. */
    fun next(): StoryRound = copy(run = run.next())
}

/**
 * Story strip: the stories of `stories` in practice.json. It opens once a story has all its Items
 * learned; a Round is up to [STORIES] stories, those not played today first.
 */
internal object StoryStrip : GameRules {
    const val WAY = "story"
    const val LISTEN = "listen"
    const val READ = "read"

    /** How many stories a Round tells, at most. */
    const val STORIES = 3

    override val game = Game.STORY_STRIP

    private fun stories(learned: Learned, listen: Boolean): List<Story> {
        val known = learned.items.map { it.id }.toSet()
        return learned.course.stories
            .filter { story -> story.panels.all { known.containsAll(it.items) && it.picture in learned.course.pictures } }
            .filter { !listen || learned.course.clipFor(it.text) != null }
    }

    override fun state(learned: Learned, sound: Boolean): GameState =
        if (stories(learned, listen = false).isNotEmpty()) GameState.READY else GameState.NEEDS_MORE

    override fun choices(learned: Learned, wanted: List<String>, sound: Boolean): List<ChoiceView> {
        val ways = listOfNotNull(LISTEN.takeIf { sound && stories(learned, listen = true).isNotEmpty() }, READ)
        return listOf(choice(WAY, ways.map { OptionView(it) }, wanted.getOrNull(0)))
    }

    override fun start(learned: Learned, selected: List<String>, sound: Boolean, random: Random, seen: Set<String>, avoidFirst: String?): Round {
        val listen = selected[0] == LISTEN
        val chosen = random.draw(stories(learned, listen), STORIES, seen, avoidFirst) { it.text }
        val views = chosen.map { story ->
            // The tiles in a mixed order, never the story's own: that would be the answer already.
            val shown = story.panels.indices.shuffled(random).let { mixed ->
                if (mixed == story.panels.indices.toList()) mixed.drop(1) + mixed.first() else mixed
            }
            StoryView(
                key = story.text,
                title = learned.localize(story.title),
                text = story.text,
                tiles = shown.map { panel ->
                    val it = story.panels[panel]
                    StoryTile(learned.course.pictures.getValue(it.picture), it.text, learned.localize(it.meaning))
                },
                order = story.panels.indices.map { shown.indexOf(it) },
            )
        }
        val questions = views.flatMapIndexed { index, view -> view.order.mapIndexed { place, tile -> StoryQuestion(index, place, tile) } }
        return StoryRound(selected, listen, views, Run(questions))
    }
}
