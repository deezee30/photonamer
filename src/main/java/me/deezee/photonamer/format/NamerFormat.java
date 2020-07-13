/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.format;

import com.google.common.collect.ImmutableMap;
import me.deezee.photonamer.process.NamerProcessException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

import java.nio.file.Path;
import java.text.DateFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static java.time.temporal.ChronoField.*;

public class NamerFormat {

    public static final String[] DEFAULT_ALLOWED_EXTS = {"jpg", "jpeg", "gif", "png", "bmp"};

    private static final ImmutableMap<Var, NamerFormatCondition> VARS = new ImmutableMap.Builder<Var, NamerFormatCondition>()
            // ID of item being renamed within the process
            .put(Var.ID,            (id, attr) -> String.valueOf(id))

            // File creation date
            .put(Var.C_YEAR,        (id, attr) -> attr.createdAt(YEAR))
            .put(Var.C_MONTH_ID_A,  (id, attr) -> academicMonthId(attr.createdAt(MONTH_OF_YEAR)))
            .put(Var.C_MONTH_ID,    (id, attr) -> attr.createdAt(MONTH_OF_YEAR))
            .put(Var.C_MONTH,       (id, attr) -> month(attr.createdAt(MONTH_OF_YEAR)))
            .put(Var.C_DAY,         (id, attr) -> attr.createdAt(DAY_OF_MONTH))
            .put(Var.C_HOUR_AMPM,   (id, attr) -> attr.createdAt(HOUR_OF_AMPM))
            .put(Var.C_HOUR,        (id, attr) -> attr.createdAt(HOUR_OF_DAY))
            .put(Var.C_MINUTE,      (id, attr) -> attr.createdAt(MINUTE_OF_HOUR))
            .put(Var.C_SECOND,      (id, attr) -> attr.createdAt(SECOND_OF_MINUTE))
            .put(Var.C_MILLI,       (id, attr) -> attr.createdAt(MILLI_OF_SECOND))
            .put(Var.C_AMPM_HI,     (id, attr) -> attr.createdAt(AMPM_OF_DAY) == 0 ? "AM" : "PM")
            .put(Var.C_AMPM_LO,     (id, attr) -> attr.createdAt(AMPM_OF_DAY) == 0 ? "am" : "pm")

            // File last modification date
            .put(Var.M_YEAR,        (id, attr) -> attr.modifiedAt(YEAR))
            .put(Var.M_MONTH_ID_A,  (id, attr) -> academicMonthId(attr.modifiedAt(MONTH_OF_YEAR)))
            .put(Var.M_MONTH_ID,    (id, attr) -> attr.modifiedAt(MONTH_OF_YEAR))
            .put(Var.M_MONTH,       (id, attr) -> month(attr.modifiedAt(MONTH_OF_YEAR)))
            .put(Var.M_DAY,         (id, attr) -> attr.modifiedAt(DAY_OF_MONTH))
            .put(Var.M_HOUR_AMPM,   (id, attr) -> attr.modifiedAt(HOUR_OF_AMPM))
            .put(Var.M_HOUR,        (id, attr) -> attr.modifiedAt(HOUR_OF_DAY))
            .put(Var.M_MINUTE,      (id, attr) -> attr.modifiedAt(MINUTE_OF_HOUR))
            .put(Var.M_SECOND,      (id, attr) -> attr.modifiedAt(SECOND_OF_MINUTE))
            .put(Var.M_MILLI,       (id, attr) -> attr.modifiedAt(MILLI_OF_SECOND))
            .put(Var.M_AMPM_HI,     (id, attr) -> attr.modifiedAt(AMPM_OF_DAY) == 0 ? "AM" : "PM")
            .put(Var.M_AMPM_LO,     (id, attr) -> attr.modifiedAt(AMPM_OF_DAY) == 0 ? "am" : "pm")

            // Image taken date
            .put(Var.T_YEAR,        (id, attr) -> attr.takenAt(YEAR).orElse(attr.createdAt(YEAR)))
            .put(Var.T_MONTH_ID_A,  (id, attr) -> academicMonthId(attr.takenAt(MONTH_OF_YEAR).orElse(attr.createdAt(MONTH_OF_YEAR))))
            .put(Var.T_MONTH_ID,    (id, attr) -> attr.takenAt(MONTH_OF_YEAR).orElse(attr.createdAt(MONTH_OF_YEAR)))
            .put(Var.T_MONTH,       (id, attr) -> month(attr.takenAt(MONTH_OF_YEAR).orElse(attr.createdAt(MONTH_OF_YEAR))))
            .put(Var.T_DAY,         (id, attr) -> attr.takenAt(DAY_OF_MONTH).orElse(attr.createdAt(DAY_OF_MONTH)))
            .put(Var.T_HOUR_AMPM,   (id, attr) -> attr.takenAt(HOUR_OF_AMPM).orElse(attr.createdAt(HOUR_OF_AMPM)))
            .put(Var.T_HOUR,        (id, attr) -> attr.takenAt(HOUR_OF_DAY).orElse(attr.createdAt(HOUR_OF_DAY)))
            .put(Var.T_MINUTE,      (id, attr) -> attr.takenAt(MINUTE_OF_HOUR).orElse(attr.createdAt(MINUTE_OF_HOUR)))
            .put(Var.T_SECOND,      (id, attr) -> attr.takenAt(SECOND_OF_MINUTE).orElse(attr.createdAt(SECOND_OF_MINUTE)))
            .put(Var.T_MILLI,       (id, attr) -> attr.takenAt(MILLI_OF_SECOND).orElse(attr.createdAt(MILLI_OF_SECOND)))
            .put(Var.T_AMPM_HI,     (id, attr) -> attr.takenAt(AMPM_OF_DAY).orElse(attr.createdAt(AMPM_OF_DAY)) == 0 ? "AM" : "PM")
            .put(Var.T_AMPM_LO,     (id, attr) -> attr.takenAt(AMPM_OF_DAY).orElse(attr.createdAt(AMPM_OF_DAY)) == 0 ? "am" : "pm")

            // TODO: Image taken location

            .build();

    private final String format;
    private Var groupBy;

    public NamerFormat(String name, Var groupBy) throws NamerProcessException {
        this.format = Validate.notNull(name, "File formatting cannot be null.");
        setGrouping(groupBy);

        // Validate that all characters are legal
        try {
            Path.of(format);
        } catch (Exception e) {
            throw new NamerProcessException(e);
        }
    }

    public String getFormat() {
        return format;
    }

    public void setGrouping(Var groupBy) throws NamerProcessException {
        if (!ArrayUtils.contains(Var.availableGroupings(), groupBy))
            throw new NamerProcessException("Unsupported image grouping variable '" + groupBy.getVariable()
                    +  "'. Available variables: " + Arrays.toString(Var.availableGroupings()));

        this.groupBy = groupBy;
    }

    public Optional<Var> getGrouping() {
        return Optional.ofNullable(groupBy);
    }

    public static Map<Var, NamerFormatCondition> getVariables() {
        return VARS;
    }

    private static int fromMillis(long millis, ChronoField unit) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).get(unit);
    }

    private static int academicMonthId(int monthId) {
        // Shift the month IDs to start from september
        return monthId < 9 ? monthId + 4 : monthId - 8;
    }

    private static String month(int monthId) {
        return new DateFormatSymbols().getMonths()[monthId - 1];
    }

    public enum Var {
        // Misc
        ID            ("seq_id",    "The sequential ID of the photo."),

        // Photo creation date
        C_YEAR        ("c_year",    "File creation year."),
        C_MONTH_ID_A  ("c_mon_id2", "File creation academic month ID of year"),
        C_MONTH_ID    ("c_mon_id",  "File creation month ID of year."),
        C_MONTH       ("c_mon",     "File creation month of year"),
        C_DAY         ("c_day",     "File creation day of month."),
        C_HOUR_AMPM   ("c_hour2",   "File creation hour of day (in AM/PM format)."),
        C_HOUR        ("c_hour",    "File creation hour of day (in 24-hour format)."),
        C_MINUTE      ("c_min",     "File creation minute of hour."),
        C_SECOND      ("c_sec",     "File creation second of minute."),
        C_MILLI       ("c_mil",     "File creation millisecond of second."),
        C_AMPM_HI     ("c_AMPM",    "File creation \"AM\" or \"PM\"."),
        C_AMPM_LO     ("c_ampm",    "File creation \"am\" or \"pm\"."),

        // Photo modification date
        M_YEAR        ("m_year",    "File last modification year."),
        M_MONTH_ID_A  ("m_mon_id2", "File last modification academic month ID of year"),
        M_MONTH_ID    ("m_mon_id",  "File last modification month ID of year."),
        M_MONTH       ("m_mon",     "File last modification month of year"),
        M_DAY         ("m_day",     "File last modification day of month."),
        M_HOUR_AMPM   ("m_hour2",   "File last modification hour of day (in AM/PM format)."),
        M_HOUR        ("m_hour",    "File last modification hour of day (in 24-hour format)."),
        M_MINUTE      ("m_min",     "File last modification minute of hour."),
        M_SECOND      ("m_sec",     "File last modification second of minute."),
        M_MILLI       ("m_mil",     "File last modification millisecond of second."),
        M_AMPM_HI     ("m_AMPM",    "File last modification \"AM\" or \"PM\"."),
        M_AMPM_LO     ("m_ampm",    "File last modification \"am\" or \"pm\"."),

        // Photo taken date
        T_YEAR        ("t_year",    "Photo taken year."),
        T_MONTH_ID_A  ("t_mon_id2", "Photo taken academic month ID of year"),
        T_MONTH_ID    ("t_mon_id",  "Photo taken month ID of year."),
        T_MONTH       ("t_mon",     "Photo taken month of year"),
        T_DAY         ("t_day",     "Photo taken day of month."),
        T_HOUR_AMPM   ("t_hour2",   "Photo taken hour of day (in AM/PM format)."),
        T_HOUR        ("t_hour",    "Photo taken hour of day (in 24-hour format)."),
        T_MINUTE      ("t_min",     "Photo taken minute of hour."),
        T_SECOND      ("t_sec",     "Photo taken second of minute."),
        T_MILLI       ("t_mil",     "Photo taken millisecond of second."),
        T_AMPM_HI     ("t_AMPM",    "Photo taken \"AM\" or \"PM\"."),
        T_AMPM_LO     ("t_ampm",    "Photo taken \"am\" or \"pm\".");

        private final String name;
        private final String description;

        Var(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getVariable() {
            return "$" + name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return getName();
        }

        public static Var[] availableGroupings() {
            return new Var[] {  null,   C_YEAR, C_MONTH,    C_MONTH_ID, C_MONTH_ID_A,
                                        M_YEAR, M_MONTH,    M_MONTH_ID, M_MONTH_ID_A,
                                        T_YEAR, T_MONTH,    T_MONTH_ID, T_MONTH_ID_A};
        }
    }
}