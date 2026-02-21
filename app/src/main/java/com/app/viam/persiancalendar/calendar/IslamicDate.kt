package com.app.viam.persiancalendar.calendar

import com.app.viam.persiancalendar.calendar.util.TwelveMonthsYear.monthStartOfMonthsDistance
import com.app.viam.persiancalendar.calendar.util.TwelveMonthsYear.monthsDistanceTo
import com.app.viam.persiancalendar.calendar.islamic.FallbackIslamicConverter
import com.app.viam.persiancalendar.calendar.islamic.IranianIslamicDateConverter
import com.app.viam.persiancalendar.calendar.islamic.UmmAlQuraConverter

/**
 * @author Amir
 */
class IslamicDate : AbstractDate, YearMonthDate<IslamicDate> {
    constructor(year: Int, month: Int, dayOfMonth: Int) : super(year, month, dayOfMonth)
    constructor(date: AbstractDate) : super(date)
    constructor(jdn: Long) : super(jdn)

    override fun toJdn(): Long {
        val year = year
        val month = month
        val day = dayOfMonth

        val tableResult: Long =
            if (useUmmAlQura) UmmAlQuraConverter.toJdn(year, month, day).toLong()
            else IranianIslamicDateConverter.toJdn(year, month, day)

        return if (tableResult != -1L) tableResult - islamicOffset
        else FallbackIslamicConverter.toJdn(year, month, day) - islamicOffset
    }

    override fun fromJdn(jdn: Long): DateTriplet {
        var jdn = jdn
        jdn += islamicOffset.toLong()
        return (if (useUmmAlQura) {
            UmmAlQuraConverter.fromJdn(jdn)
        } else {
            IranianIslamicDateConverter.fromJdn(jdn)
        }) ?: FallbackIslamicConverter.fromJdn(jdn)
    }

    override fun monthStartOfMonthsDistance(monthsDistance: Int): IslamicDate =
        monthStartOfMonthsDistance(this, monthsDistance, ::IslamicDate)

    override fun monthsDistanceTo(date: IslamicDate): Int = monthsDistanceTo(this, date)

    override fun toString(): String = "IslamicDate($year, $month, $dayOfMonth)"

    companion object {
        // Converters
        var useUmmAlQura = false
        var islamicOffset = 0
    }
}
