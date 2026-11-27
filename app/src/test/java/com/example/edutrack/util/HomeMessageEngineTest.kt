package com.example.edutrack.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class HomeMessageEngineTest {

    @Test
    fun `pending reminders take highest priority`() {
        val single = HomeMessageEngine.selectMessage(
            HomeMessageContext(
                localTime = LocalTime.of(9, 0),
                pendingDueRemindersCount = 1,
                todayMinutes = 200,
                todaySessionCount = 3,
                totalDailyGoalMinutes = 120,
                currentStreakDays = 5,
                daysSinceLastStudy = 0
            ),
            variantSeed = 0
        )
        assertEquals(HomeMessageCategory.PENDING_REMINDERS, single.category)

        val multiple = HomeMessageEngine.selectMessage(
            HomeMessageContext(
                localTime = LocalTime.of(9, 0),
                pendingDueRemindersCount = 3,
                todayMinutes = 200,
                todaySessionCount = 3,
                totalDailyGoalMinutes = 120,
                currentStreakDays = 5,
                daysSinceLastStudy = 0
            ),
            variantSeed = 0
        )
        assertEquals(HomeMessageCategory.PENDING_REMINDERS, multiple.category)
        assertTrue(multiple.headline.contains("3"))
    }

    @Test
    fun `goal complete takes priority over long study and studied today`() {
        val result = HomeMessageEngine.selectMessage(
            HomeMessageContext(
                localTime = LocalTime.of(14, 0),
                pendingDueRemindersCount = 0,
                todayMinutes = 120,
                todaySessionCount = 2,
                totalDailyGoalMinutes = 90,
                currentStreakDays = 4,
                daysSinceLastStudy = 0
            ),
            variantSeed = 1
        )
        assertEquals(HomeMessageCategory.GOAL_COMPLETE, result.category)
    }

    @Test
    fun `long study triggers when goal not met but threshold reached`() {
        val result = HomeMessageEngine.selectMessage(
            HomeMessageContext(
                localTime = LocalTime.of(19, 0),
                pendingDueRemindersCount = 0,
                todayMinutes = 125,
                todaySessionCount = 2,
                totalDailyGoalMinutes = 180,
                currentStreakDays = 1,
                daysSinceLastStudy = 0
            ),
            variantSeed = 2
        )
        assertEquals(HomeMessageCategory.LONG_STUDY, result.category)
    }

    @Test
    fun `time of day messages work when no session completed today`() {
        val morning = HomeMessageEngine.selectMessage(
            HomeMessageContext(LocalTime.of(8, 30), 0, 0, 0, 60, 0, 1)
        )
        val afternoon = HomeMessageEngine.selectMessage(
            HomeMessageContext(LocalTime.of(13, 15), 0, 0, 0, 60, 0, 1)
        )
        val evening = HomeMessageEngine.selectMessage(
            HomeMessageContext(LocalTime.of(18, 45), 0, 0, 0, 60, 0, 1)
        )
        val night = HomeMessageEngine.selectMessage(
            HomeMessageContext(LocalTime.of(22, 10), 0, 0, 0, 60, 0, 1)
        )

        assertEquals(HomeMessageCategory.MORNING_FRESH, morning.category)
        assertEquals(HomeMessageCategory.AFTERNOON_FRESH, afternoon.category)
        assertEquals(HomeMessageCategory.EVENING_FRESH, evening.category)
        assertEquals(HomeMessageCategory.NIGHT_FRESH, night.category)
    }

    @Test
    fun `streak and several days away trigger when no session today`() {
        val streak = HomeMessageEngine.selectMessage(
            HomeMessageContext(LocalTime.of(10, 0), 0, 0, 0, 60, 4, 1),
            variantSeed = 0
        )
        assertEquals(HomeMessageCategory.STREAK_ACTIVE, streak.category)
        assertTrue(streak.supportingLine.contains("4"))

        val away = HomeMessageEngine.selectMessage(
            HomeMessageContext(LocalTime.of(10, 0), 0, 0, 0, 60, 0, 5),
            variantSeed = 0
        )
        assertEquals(HomeMessageCategory.AWAY_SEVERAL_DAYS, away.category)
    }
}
