package core.framework.plugin.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author ebin
 */
public final class ClassUtils {
    public static final String ENUM = Enum.class.getCanonicalName();
    public static final String STRING = String.class.getCanonicalName();
    public static final String INTEGER = Integer.class.getCanonicalName();
    public static final String BOOLEAN = Boolean.class.getCanonicalName();
    public static final String LONG = Long.class.getCanonicalName();
    public static final String DOUBLE = Double.class.getCanonicalName();
    public static final String LIST_CLASS = List.class.getCanonicalName();
    public static final String SET_CLASS = Set.class.getCanonicalName();
    public static final String ZONED_DATE_TIME = ZonedDateTime.class.getCanonicalName();
    public static final String LOCAL_DATE_TIME = LocalDateTime.class.getCanonicalName();
    public static final String LOCAL_DATE = LocalDate.class.getCanonicalName();
    public static final String BIG_DECIMAL = BigDecimal.class.getCanonicalName();
    public static final String STREAM = Stream.class.getCanonicalName();

    public static final String APP_CLASS = "app.";

    private ClassUtils() {
    }

    public static boolean isInteger(String type) {
        return type.contains(INTEGER);
    }

    public static boolean isLong(String type) {
        return type.contains(LONG);
    }

    public static boolean isBoolean(String type) {
        return type.contains(BOOLEAN);
    }

    public static boolean isZonedDateTime(String type) {
        return type.contains(ZONED_DATE_TIME);
    }

    public static boolean isLocalDateTime(String type) {
        return type.contains(LOCAL_DATE_TIME);
    }

    public static boolean isLocalDate(String type) {
        return type.contains(LOCAL_DATE);
    }

    public static boolean isString(String type) {
        return type.contains(STRING);
    }

    public static boolean isEnum(String type) {
        return type.contains(ENUM);
    }

    public static boolean isList(String type) {
        return type.contains(LIST_CLASS);
    }

    public static boolean isDouble(String type) {
        return type.contains(DOUBLE);
    }

    public static boolean isBigDecimal(String type) {
        return type.contains(BIG_DECIMAL);
    }

    public static boolean isSet(String type) {
        return type.contains(SET_CLASS);
    }

    public static boolean isStream(String type) {
        return type.contains(STREAM);
    }

    public static boolean isJavaBean(String type) {
        return type.startsWith(APP_CLASS);
    }
}
