package ai.tambo.tmodeler.field.advanced

import kotlinx.datetime.*
import ai.tambo.tmodeler.field.base.TField
import ai.tambo.tmodeler.field.base.TFtype
import ai.tambo.tmodeler.field.common.TextField

class TimeField : TField<LocalDateTime, TimeField>(TFtype.TEXT) {
    enum class Format { DATE, DATETIME }

    private var format: Format = Format.DATETIME
    private val depField = TextField()
    private var dep by depField

    fun set(v: String) {
        setValue(
            parseTemporal(v)
        )
    }

    fun format(f: Format): TimeField {
        format = f
        return this
    }

    // `get()` renvoie null sans lever : une exception par lecture est très coûteuse en wasm (stack trace)
    override fun getValue(): Any? = depField.get()
    override fun setValue(v: Any?) {
        val str = when (v) {
            is String -> v
            is LocalDateTime -> when (format) {
                Format.DATE -> v.date.toString()
                Format.DATETIME -> v.toString()
            }
            is LocalDate -> when (format) {
                Format.DATE -> v.toString()
                Format.DATETIME -> LocalDateTime(v, LocalTime(0, 0)).toString() // ajouter 00:00
            }
            else -> ""
        }
        dep = str
        _value = parseTemporal(str)
    }

    /*private fun parseTemporal(text: String): Any? {
        return try {
            when (format) {
                Format.DATE -> LocalDate.parse(text)
                Format.DATETIME -> LocalDateTime.parse(text)
            }
        } catch (e: Exception) {
            null
        }
    }*/
    private fun parseTemporal(text: String): Any? {
        return try {
            when (format) {
                Format.DATE -> LocalDate.parse(text)

                Format.DATETIME -> {
                    try {
                        // Cas : 2026-07-14T16:46:25
                        LocalDateTime.parse(text)
                    } catch (_: Exception) {
                        // Cas : 2026-07-14T16:46:25.020606Z
                        Instant.parse(text)
                            .toLocalDateTime(TimeZone.UTC)
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object{
        fun now() : LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        fun utc() : LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        fun toString(time: LocalDateTime?): String {
            if (time == null) return " - - - "

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            return if (time.year == now.year &&
                time.month == now.month &&
                time.dayOfMonth == now.dayOfMonth) {
                // Aujourd'hui : afficher hh:mm
                "${time.hour}:${time.minute.toString().padStart(2, '0')}"
            } else {
                // Jour passé : afficher dd/MM hh:mm
                "${time.dayOfMonth}/${time.month.ordinal + 1} ${time.hour}:${time.minute.toString().padStart(2, '0')}"
            }
        }
    }

    override fun toString(): String {
        return if(exists()) toString(min = Unit.YEAR, max = Unit.SECOND)
        else super.toString()
    }

    enum class Unit {
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MINUTE,
        SECOND,
        MICROSECOND
    }

    enum class Context {
        RAW,        // 2026-07-14 16:46:25
        DOCUMENT,   // 14/07/2026 16:46
        CHAT,       // 16:46, Hier, Lundi...
        SOCIAL,     // il y a 5 min
        CALENDAR    // Aujourd'hui, Demain...
    }

    private fun Int.pad(width: Int): String =
        toString().padStart(width, '0')

    fun toString(
        min: Unit = Unit.YEAR,
        max: Unit = Unit.SECOND,
        context: Context = Context.RAW
    ): String {

        val value = _value ?: return ""

        return when (context) {
            Context.RAW -> formatAbsolute(value, min, max)

            Context.DOCUMENT -> formatAbsolute(value, min, max)

            Context.CHAT -> formatChat(value)

            Context.SOCIAL -> formatRelative(value)

            Context.CALENDAR -> formatCalendar(value)
        }
    }

    private fun formatAbsolute(
        value: Any,
        min: Unit,
        max: Unit
    ): String {

        val date: LocalDate
        var time: LocalTime? = null

        when (value) {
            is LocalDate -> date = value
            is LocalDateTime -> {
                date = value.date
                time = value.time
            }
            else -> return ""
        }

        val sb = StringBuilder()

        if (min <= Unit.YEAR && max >= Unit.YEAR)
            sb.append(date.year.pad(4))

        if (min <= Unit.MONTH && max >= Unit.MONTH) {
            if (sb.isNotEmpty()) sb.append('-')
            sb.append(date.monthNumber.pad(2))
        }

        if (min <= Unit.DAY && max >= Unit.DAY) {
            if (sb.isNotEmpty()) sb.append('-')
            sb.append(date.dayOfMonth.pad(2))
        }

        if (time == null || max < Unit.HOUR)
            return sb.toString()

        sb.append(' ')
        sb.append(time.hour.pad(2))

        if (max >= Unit.MINUTE) {
            sb.append(':')
            sb.append(time.minute.pad(2))
        }

        if (max >= Unit.SECOND) {
            sb.append(':')
            sb.append(time.second.pad(2))
        }

        if (max >= Unit.MICROSECOND && time.nanosecond != 0) {
            sb.append('.')
            sb.append((time.nanosecond / 1_000).pad(6))
        }

        return sb.toString()
    }

    private fun formatChat(value: Any): String {

        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())

        val dateTime = when (value) {
            is LocalDate -> value.atTime(0, 0)
            is LocalDateTime -> value
            else -> return ""
        }

        val today = now.date
        val yesterday = today.minus(DatePeriod(days = 1))
        val tomorrow = today.plus(DatePeriod(days = 1))

        return when (dateTime.date) {

            today ->
                "${dateTime.hour.pad(2)}:${dateTime.minute.pad(2)}"

            yesterday ->
                "Hier"

            tomorrow ->
                "Demain"

            else ->
                formatAbsolute(value, Unit.YEAR, Unit.MINUTE)
        }
    }

    private fun formatRelative(value: Any): String {

        val instant = when (value) {
            is LocalDateTime ->
                value.toInstant(TimeZone.currentSystemDefault())

            else ->
                return formatAbsolute(value, Unit.YEAR, Unit.DAY)
        }

        val diff = Clock.System.now() - instant

        val sec = diff.inWholeSeconds
        val min = diff.inWholeMinutes
        val hour = diff.inWholeHours
        val day = diff.inWholeDays

        return when {
            sec < 10 -> "À l'instant"
            sec < 60 -> "Il y a ${sec}s"
            min < 60 -> "Il y a ${min} min"
            hour < 24 -> "Il y a ${hour} h"
            day == 1L -> "Hier"
            day < 7 -> "Il y a ${day} jours"
            else -> formatAbsolute(value, Unit.YEAR, Unit.DAY)
        }
    }

    private fun formatCalendar(value: Any): String {

        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())

        val date = when (value) {
            is LocalDate -> value
            is LocalDateTime -> value.date
            else -> return ""
        }

        return when (date) {

            now.date ->
                "Aujourd'hui"

            now.date.minus(DatePeriod(days = 1)) ->
                "Hier"

            now.date.plus(DatePeriod(days = 1)) ->
                "Demain"

            else ->
                formatAbsolute(value, Unit.YEAR, Unit.DAY)
        }
    }

    /*
    fun toString(
        min: Unit = Unit.YEAR,
        max: Unit = Unit.SECOND
    ): String {

        val value = _value ?: return ""

        val date: LocalDate
        var time: kotlinx.datetime.LocalTime? = null

        when (value) {
            is LocalDate -> {
                date = value
            }

            is LocalDateTime -> {
                date = value.date
                time = value.time
            }

            else -> return ""
        }

        val result = StringBuilder()

        // Date
        if (min <= Unit.YEAR && max >= Unit.YEAR) {
            result.append(date.year.pad(4))
        }

        if (min <= Unit.MONTH && max >= Unit.MONTH) {
            if (result.isNotEmpty()) result.append('-')
            result.append(date.monthNumber.pad(2))
        }

        if (min <= Unit.DAY && max >= Unit.DAY) {
            if (result.isNotEmpty()) result.append('-')
            result.append(date.dayOfMonth.pad(2))
        }

        if (time == null || max < Unit.HOUR) {
            return result.toString()
        }

        // Heure
        result.append(' ')

        result.append(time.hour.pad(2))

        if (max >= Unit.MINUTE) {
            result.append(':')
            result.append(time.minute.pad(2))
        }

        if (max >= Unit.SECOND) {
            result.append(':')
            result.append(time.second.pad(2))
        }

        if (max >= Unit.MICROSECOND && time.nanosecond != 0) {
            result.append('.')
            result.append((time.nanosecond / 1_000).pad(6))
        }

        return result.toString()
    }
    */

}


fun LocalDateTime.plusHours(hours: Int): LocalDateTime =
    shift(hours, DateTimeUnit.HOUR)

fun LocalDateTime.minusHours(hours: Int): LocalDateTime =
    shift(-hours, DateTimeUnit.HOUR)

fun LocalDateTime.plusMinutes(minutes: Int): LocalDateTime =
    shift(minutes, DateTimeUnit.MINUTE)

fun LocalDateTime.minusMinutes(minutes: Int): LocalDateTime =
    shift(-minutes, DateTimeUnit.MINUTE)

fun LocalDateTime.shift(
    value: Int,
    unit: DateTimeUnit.TimeBased
): LocalDateTime {

    val tz = TimeZone.currentSystemDefault()

    val instant = this.toInstant(tz)
        .plus(value, unit)

    return instant.toLocalDateTime(tz)
}

fun LocalDateTime.copy(
    year: Int = this.year,
    monthNumber: Int = this.monthNumber,
    dayOfMonth: Int = this.dayOfMonth,
    hour: Int = this.hour,
    minute: Int = this.minute,
    second: Int = this.second,
    nano: Int = this.nanosecond
): LocalDateTime {
    return LocalDateTime(
        year = year,
        monthNumber = monthNumber,
        dayOfMonth = dayOfMonth,
        hour = hour,
        minute = minute,
        second = second,
        nanosecond = nano
    )
}