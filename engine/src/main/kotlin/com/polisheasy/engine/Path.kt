package com.polisheasy.engine

/** The Path as the Learner sees it: every Level opened so far, in order. */
data class PathView(val levels: List<LevelView>)

data class LevelView(val id: String, val title: String, val units: List<UnitView>)

/** [number] counts the Level's Units from 1. */
data class UnitView(val id: String, val number: Int, val title: String, val dailySets: List<DailySetView>)

/**
 * [number] counts the Level's Daily Sets from 1, across its Units. [title] is the title of its
 * first Tip, which names what the day is about, or null when it has no Tip.
 */
data class DailySetView(val id: String, val number: Int, val state: DailySetState, val title: String? = null)

enum class DailySetState {
    /** Done, and can be replayed. */
    COMPLETED,

    /** The one the Learner can start or continue today. */
    TODAY,

    /** Not reachable yet. */
    LOCKED,
}
