package de.storchp.opentracks.osmplugin.utils

import android.content.Context
import android.text.format.DateUtils
import de.storchp.opentracks.osmplugin.R
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.time.Duration

/**
 * Various string manipulation methods.
 */
object StringUtils {
    /**
     * Formats the elapsed time in the form "H:MM".
     */
    fun formatElapsedTimeHoursMinutes(context: Context, duration: Duration) =
        context.getString(
            R.string.stat_time,
            DateUtils.formatElapsedTime(duration.inWholeSeconds)
        )

    fun formatDistanceInKm(context: Context, distanceMeter: Double) =
        context.getString(
            R.string.stat_distance_with_unit,
            formatDecimal(distanceMeter * UnitConversions.M_TO_KM),
            context.getString(R.string.unit_kilometer)
        )

    fun formatDistanceInMi(context: Context, distanceMeter: Double) =
        context.getString(
            R.string.stat_distance_with_unit,
            formatDecimal(distanceMeter * UnitConversions.M_TO_MI),
            context.getString(R.string.unit_mile)
        )

    fun formatSpeedInKmPerHour(context: Context, meterPerSeconds: Double) =
        context.getString(
            R.string.stat_distance_with_unit,
            formatDecimal(meterPerSeconds * UnitConversions.MS_TO_KMH),
            context.getString(R.string.unit_kilometer_per_hour)
        )

    fun formatSpeedInMiPerHour(context: Context, meterPerSeconds: Double) =
        context.getString(
            R.string.stat_distance_with_unit,
            formatDecimal(meterPerSeconds * UnitConversions.MS_TO_KMH * UnitConversions.KM_TO_MI),
            context.getString(R.string.unit_mile_per_hour)
        )

    fun formatPaceMinPerKm(context: Context, meterPerSeconds: Double): String {
        return formatPace(
            context, 
            meterPerSeconds, 
            context.getString(R.string.unit_minute_per_kilometer),
            1.0 // No conversion needed for kilometers
        )
    }

    fun formatPaceMinPerMi(context: Context, meterPerSeconds: Double): String {
        return formatPace(
            context, 
            meterPerSeconds, 
            context.getString(R.string.unit_minute_per_mile),
            UnitConversions.KM_TO_MI // Conversion from kilometers to miles
        )
    }

    /**
     * Common method for formatting pace.
     */
    private fun formatPace(context: Context, meterPerSeconds: Double, unit: String, conversionFactor: Double): String {
        if (meterPerSeconds == 0.0) {
            return "0:00"
        }
        val kmPerSecond = (meterPerSeconds / UnitConversions.KM_TO_M) * conversionFactor
        val secondsPerKm = (1 / kmPerSecond).toInt()
        val minutes = (secondsPerKm / UnitConversions.MIN_TO_S).toInt()
        val seconds = secondsPerKm % UnitConversions.MIN_TO_S.toInt()

        return context.getString(
            R.string.stat_distance_with_unit,
            context.getString(R.string.stat_minute_seconds, minutes, seconds),
            unit
        )
    }

    private fun formatDecimal(value: Double) = decimalFormat2.format(value)

    private val decimalFormat2 = DecimalFormat().apply {
        setMinimumFractionDigits(2)
        setMaximumFractionDigits(2)
        setRoundingMode(RoundingMode.HALF_EVEN)
    }

    fun formatAltitudeChangeInMeter(context: Context, altitudeInMeter: Double) =
        context.getString(
            R.string.stat_altitude_with_unit,
            altitudeInMeter.toInt().toString(),
            context.getString(R.string.unit_meter)
        )

    fun formatAltitudeChangeInFeet(context: Context, altitudeInMeter: Double) =
        context.getString(
            R.string.stat_altitude_with_unit,
            (altitudeInMeter * UnitConversions.M_TO_FT).toInt().toString(),
            context.getString(R.string.unit_feet)
        )
}
