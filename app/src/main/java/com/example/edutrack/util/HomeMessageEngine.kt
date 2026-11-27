package com.example.edutrack.util

import java.time.LocalTime

/**
 * Semantic category for the dynamic Home Screen header message.
 */
enum class HomeMessageCategory {
    PENDING_REMINDERS,
    GOAL_COMPLETE,
    LONG_STUDY,
    STUDIED_TODAY,
    STREAK_ACTIVE,
    AWAY_SEVERAL_DAYS,
    MORNING_FRESH,
    AFTERNOON_FRESH,
    EVENING_FRESH,
    NIGHT_FRESH
}

/**
 * Action hint shown as a compact interactive pill under the dynamic Home message.
 */
enum class HomeMessageAction {
    OPEN_REMINDERS,
    OPEN_SUBJECTS,
    OPEN_PROGRESS
}

/**
 * Dynamic headline + supporting line rendered at the top of the Home Dashboard.
 */
data class HomeDynamicMessage(
    val headline: String,
    val supportingLine: String,
    val category: HomeMessageCategory,
    val statusBadge: String? = null,
    val actionLabel: String? = null,
    val action: HomeMessageAction? = null
)

/**
 * Snapshot of local study data and device time used to select a [HomeDynamicMessage].
 */
data class HomeMessageContext(
    val localTime: LocalTime,
    val pendingDueRemindersCount: Int,
    val todayMinutes: Int,
    val todaySessionCount: Int,
    val totalDailyGoalMinutes: Int,
    val currentStreakDays: Int,
    val daysSinceLastStudy: Int?
)

/**
 * Pure offline engine that selects a supportive, non-shaming dynamic message
 * for the Home Dashboard from local study state and device time.
 */
object HomeMessageEngine {

    const val LONG_STUDY_THRESHOLD_MINUTES = 120

    fun selectMessage(
        context: HomeMessageContext,
        variantSeed: Int = 0
    ): HomeDynamicMessage {
        val safeSeed = if (variantSeed < 0) -variantSeed else variantSeed

        // 1. Pending due/overdue study reminders first
        if (context.pendingDueRemindersCount > 0) {
            return if (context.pendingDueRemindersCount > 1) {
                HomeDynamicMessage(
                    headline = "${context.pendingDueRemindersCount} study reminders are still open.",
                    supportingLine = "Pick one to start, or reschedule the rest.",
                    category = HomeMessageCategory.PENDING_REMINDERS,
                    statusBadge = "${context.pendingDueRemindersCount} due",
                    actionLabel = "View reminders",
                    action = HomeMessageAction.OPEN_REMINDERS
                )
            } else {
                val (headline, supporting) = pickVariant(PENDING_SINGLE_VARIANTS, safeSeed)
                HomeDynamicMessage(
                    headline = headline,
                    supportingLine = supporting,
                    category = HomeMessageCategory.PENDING_REMINDERS,
                    statusBadge = "1 due",
                    actionLabel = "Start or reschedule",
                    action = HomeMessageAction.OPEN_REMINDERS
                )
            }
        }

        // 2. Today's study goal reached
        if (context.totalDailyGoalMinutes > 0 && context.todayMinutes >= context.totalDailyGoalMinutes) {
            val (headline, supporting) = pickVariant(GOAL_COMPLETE_VARIANTS, safeSeed)
            return HomeDynamicMessage(
                headline = headline,
                supportingLine = supporting,
                category = HomeMessageCategory.GOAL_COMPLETE,
                statusBadge = "Goal complete",
                actionLabel = "View progress",
                action = HomeMessageAction.OPEN_PROGRESS
            )
        }

        // 3. Long study session today
        if (context.todayMinutes >= LONG_STUDY_THRESHOLD_MINUTES) {
            val (headline, supporting) = pickVariant(LONG_STUDY_VARIANTS, safeSeed)
            return HomeDynamicMessage(
                headline = headline,
                supportingLine = supporting,
                category = HomeMessageCategory.LONG_STUDY,
                statusBadge = DateTimeUtils.formatDuration(context.todayMinutes),
                actionLabel = "View progress",
                action = HomeMessageAction.OPEN_PROGRESS
            )
        }

        // 4. User has studied today
        if (context.todaySessionCount > 0) {
            val (headline, supporting) = pickVariant(STUDIED_TODAY_VARIANTS, safeSeed)
            return HomeDynamicMessage(
                headline = headline,
                supportingLine = supporting,
                category = HomeMessageCategory.STUDIED_TODAY,
                statusBadge = if (context.currentStreakDays >= 2) "${context.currentStreakDays}d streak" else null,
                actionLabel = "Keep going",
                action = HomeMessageAction.OPEN_SUBJECTS
            )
        }

        // 5 & 6. No session completed today: check several-days-away, active streak, or time of day
        if (context.daysSinceLastStudy != null && context.daysSinceLastStudy >= 3) {
            val (headline, supporting) = pickVariant(AWAY_SEVERAL_DAYS_VARIANTS, safeSeed)
            return HomeDynamicMessage(
                headline = headline,
                supportingLine = supporting,
                category = HomeMessageCategory.AWAY_SEVERAL_DAYS,
                statusBadge = "Restart gently",
                actionLabel = "Pick a subject",
                action = HomeMessageAction.OPEN_SUBJECTS
            )
        }

        if (context.currentStreakDays >= 2) {
            val (headline, supporting) = pickStreakVariant(context.currentStreakDays, safeSeed)
            return HomeDynamicMessage(
                headline = headline,
                supportingLine = supporting,
                category = HomeMessageCategory.STREAK_ACTIVE,
                statusBadge = "${context.currentStreakDays}-day streak",
                actionLabel = "Continue streak",
                action = HomeMessageAction.OPEN_SUBJECTS
            )
        }

        val hour = context.localTime.hour
        return when {
            hour in 5..11 -> {
                val (headline, supporting) = pickVariant(MORNING_VARIANTS, safeSeed)
                HomeDynamicMessage(
                    headline = headline,
                    supportingLine = supporting,
                    category = HomeMessageCategory.MORNING_FRESH,
                    statusBadge = null,
                    actionLabel = "Choose a subject",
                    action = HomeMessageAction.OPEN_SUBJECTS
                )
            }
            hour in 12..16 -> {
                val (headline, supporting) = pickVariant(AFTERNOON_VARIANTS, safeSeed)
                HomeDynamicMessage(
                    headline = headline,
                    supportingLine = supporting,
                    category = HomeMessageCategory.AFTERNOON_FRESH,
                    statusBadge = null,
                    actionLabel = "Start a session",
                    action = HomeMessageAction.OPEN_SUBJECTS
                )
            }
            hour in 17..20 -> {
                val (headline, supporting) = pickVariant(EVENING_VARIANTS, safeSeed)
                HomeDynamicMessage(
                    headline = headline,
                    supportingLine = supporting,
                    category = HomeMessageCategory.EVENING_FRESH,
                    statusBadge = null,
                    actionLabel = "Start a short session",
                    action = HomeMessageAction.OPEN_SUBJECTS
                )
            }
            else -> {
                val (headline, supporting) = pickVariant(NIGHT_VARIANTS, safeSeed)
                HomeDynamicMessage(
                    headline = headline,
                    supportingLine = supporting,
                    category = HomeMessageCategory.NIGHT_FRESH,
                    statusBadge = null,
                    actionLabel = "Set a reminder",
                    action = HomeMessageAction.OPEN_REMINDERS
                )
            }
        }
    }

    private fun pickVariant(
        variants: List<Pair<String, String>>,
        seed: Int
    ): Pair<String, String> = variants[seed % variants.size]

    private fun pickStreakVariant(streakDays: Int, seed: Int): Pair<String, String> {
        val variants = listOf(
            "You’re building a steady routine." to "You’ve studied for $streakDays days in a row.",
            "Your consistency is showing." to "Keep your $streakDays-day streak going at your own pace.",
            "Small steps, repeated often." to "You’re on a $streakDays-day study streak."
        )
        return variants[seed % variants.size]
    }

    private val MORNING_VARIANTS = listOf(
        "A fresh day, a fresh start." to "One focused session is a good beginning.",
        "Make room for your goals." to "What would you like to study today?",
        "Start small. Build momentum." to "Pick a subject and begin when you're ready."
    )

    private val AFTERNOON_VARIANTS = listOf(
        "Your day still has room to grow." to "A short session can move you forward.",
        "Take one step toward your goal." to "Choose a subject for your next focus session.",
        "A little focus goes a long way." to "Start with the time you have."
    )

    private val EVENING_VARIANTS = listOf(
        "There’s still time for a little progress." to "Try a short, focused session this evening.",
        "End the day with one small win." to "Choose a subject and get started.",
        "Your next step can be a small one." to "Even a brief review counts."
    )

    private val NIGHT_VARIANTS = listOf(
        "Plan a calmer start for tomorrow." to "Choose a subject or set a study reminder.",
        "Rest is part of a good routine." to "Your next study session can wait until tomorrow.",
        "You can pick up where you left off." to "Set a reminder for your next focus session."
    )

    private val PENDING_SINGLE_VARIANTS = listOf(
        "A study session is still on your list." to "Start it now, or move the reminder to a better time.",
        "Your plan is waiting for you." to "You can begin now or reschedule your reminder.",
        "One reminder is still open today." to "Choose a subject and take the next step."
    )

    private val STUDIED_TODAY_VARIANTS = listOf(
        "You’ve already made progress today." to "Keep going at a pace that works for you.",
        "Your focus is adding up." to "Every completed session moves you forward.",
        "Nice work showing up for your goals." to "Your progress today is saved."
    )

    private val GOAL_COMPLETE_VARIANTS = listOf(
        "You reached today’s study goal." to "Great work—take a moment to feel proud.",
        "Today’s goal: complete." to "You’ve made time for what matters.",
        "A goal met, one step at a time." to "Your effort is reflected in today’s progress."
    )

    private val LONG_STUDY_VARIANTS = listOf(
        "You’ve put in a strong study session today." to "A short break can help you recharge.",
        "That’s a lot of focused effort." to "Remember to pause, hydrate, and rest.",
        "Your hard work is adding up." to "Take a well-earned break when you need one."
    )

    private val AWAY_SEVERAL_DAYS_VARIANTS = listOf(
        "Ready to begin again?" to "One short session is enough to restart your routine.",
        "There’s no need to catch up all at once." to "Choose one subject and take a small step.",
        "A fresh start can be simple." to "Begin with a short session today."
    )
}
