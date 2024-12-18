package com.nexomc.nexo.utils

import com.google.common.collect.ComparisonChain
import java.io.Serializable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class SnapshotVersion(version: String) : Comparable<SnapshotVersion?>, Serializable {
    /**
     * Retrieve the week this snapshot was released.
     *
     * @return The week.
     */
    var snapshotDate: Date? = null

    /**
     * Retrieve the snapshot version within a week, starting at zero.
     *
     * @return The weekly version
     */
    var snapshotWeekVersion = 0

    @Transient
    private var rawString: String? = null

    init {
        val matcher = SNAPSHOT_PATTERN.matcher(version.trim { it <= ' ' })

        if (matcher.matches()) {
            try {
                this.snapshotDate = dateFormat.parse(matcher.group(1))
                this.snapshotWeekVersion = matcher.group(2)[0].code - 'a'.code
                this.rawString = version
            } catch (e: ParseException) {
                throw IllegalArgumentException("Date implied by snapshot version is invalid.", e)
            }
        } else {
            throw IllegalArgumentException("Cannot parse $version as a snapshot version.")
        }
    }

    val snapshotString: String
        /**
         * Retrieve the raw snapshot string (yy'w'ww[a-z]).
         *
         * @return The snapshot string.
         */
        get() {
            if (this.rawString == null) {
                // It's essential that we use the same locale
                val current = Calendar.getInstance(Locale.US)
                current.time = snapshotDate
                this.rawString = String.format(
                    "%02dw%02d%s",
                    current[Calendar.YEAR] % 100,
                    current[Calendar.WEEK_OF_YEAR],
                    ('a'.code + this.snapshotWeekVersion).toChar()
                )
            }
            return rawString!!
        }

    override fun compareTo(o: SnapshotVersion?): Int {
        if (o == null) {
            return 1
        }

        return ComparisonChain.start()
            .compare(this.snapshotDate, o.snapshotDate)
            .compare(this.snapshotWeekVersion, o.snapshotWeekVersion)
            .result()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }

        if (obj is SnapshotVersion) {
            val other = obj
            return this.snapshotDate == other.snapshotDate
                    && this.snapshotWeekVersion == other.snapshotWeekVersion
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(this.snapshotDate, this.snapshotWeekVersion)
    }

    override fun toString(): String {
        return this.snapshotString
    }

    companion object {
        private const val serialVersionUID = 2778655372579322310L
        private val SNAPSHOT_PATTERN = Pattern.compile("(\\d{2}w\\d{2})([a-z])")

        private val dateFormat: SimpleDateFormat
            /**
             * Retrieve the snapshot date parser.
             *
             *
             * We have to create a new instance of SimpleDateFormat every time as it is not thread safe.
             *
             * @return The date formatter.
             */
            get() {
                val format = SimpleDateFormat("yy'w'ww", Locale.US)
                format.isLenient = false
                return format
            }
    }
}
