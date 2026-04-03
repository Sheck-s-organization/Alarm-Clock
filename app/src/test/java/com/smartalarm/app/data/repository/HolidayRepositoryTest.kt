package com.smartalarm.app.data.repository

import com.smartalarm.app.data.entities.HolidayType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * Verifies that floating US federal holidays are calculated correctly for known years,
 * and that fixed holidays are all present in the ANNUAL list.
 */
class HolidayRepositoryTest {

    // --- Fixed holidays ---

    @Test
    fun fixedHolidays_containsAllFive() {
        val names = HolidayRepository.FIXED_FEDERAL_HOLIDAYS.map { it.name }
        assert("New Year's Day" in names)
        assert("Juneteenth" in names)
        assert("Independence Day" in names)
        assert("Veterans Day" in names)
        assert("Christmas Day" in names)
    }

    @Test
    fun fixedHolidays_areAllAnnual() {
        HolidayRepository.FIXED_FEDERAL_HOLIDAYS.forEach {
            assertEquals(HolidayType.ANNUAL, it.type)
        }
    }

    // --- Floating holidays for 2025 ---

    @Test
    fun mlkDay_2025_isJan20() {
        // 3rd Monday of January 2025
        assertEquals(20, HolidayRepository.nthWeekday(2025, 1, Calendar.MONDAY, 3))
    }

    @Test
    fun presidentsDay_2025_isFeb17() {
        // 3rd Monday of February 2025
        assertEquals(17, HolidayRepository.nthWeekday(2025, 2, Calendar.MONDAY, 3))
    }

    @Test
    fun memorialDay_2025_isMay26() {
        // Last Monday of May 2025
        assertEquals(26, HolidayRepository.lastWeekday(2025, 5, Calendar.MONDAY))
    }

    @Test
    fun laborDay_2025_isSep1() {
        // 1st Monday of September 2025
        assertEquals(1, HolidayRepository.nthWeekday(2025, 9, Calendar.MONDAY, 1))
    }

    @Test
    fun columbusDay_2025_isOct13() {
        // 2nd Monday of October 2025
        assertEquals(13, HolidayRepository.nthWeekday(2025, 10, Calendar.MONDAY, 2))
    }

    @Test
    fun thanksgiving_2025_isNov27() {
        // 4th Thursday of November 2025
        assertEquals(27, HolidayRepository.nthWeekday(2025, 11, Calendar.THURSDAY, 4))
    }

    // --- Spot-check a second year (2026) ---

    @Test
    fun mlkDay_2026_isJan19() {
        assertEquals(19, HolidayRepository.nthWeekday(2026, 1, Calendar.MONDAY, 3))
    }

    @Test
    fun thanksgiving_2026_isNov26() {
        assertEquals(26, HolidayRepository.nthWeekday(2026, 11, Calendar.THURSDAY, 4))
    }

    // --- floatingHolidaysForYear structure ---

    @Test
    fun floatingHolidaysForYear_returnsSixEntries() {
        assertEquals(6, HolidayRepository.floatingHolidaysForYear(2025).size)
    }

    @Test
    fun floatingHolidaysForYear_areAllOneTime() {
        HolidayRepository.floatingHolidaysForYear(2025).forEach {
            assertEquals(HolidayType.ONE_TIME, it.type)
        }
    }

    @Test
    fun floatingHolidaysForYear_allHaveCorrectYear() {
        HolidayRepository.floatingHolidaysForYear(2025).forEach {
            assertEquals(2025, it.year)
        }
    }
}
