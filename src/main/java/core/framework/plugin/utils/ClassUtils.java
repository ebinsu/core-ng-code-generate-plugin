package core.framework.plugin.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static core.framework.plugin.utils.PsiUtils.JAVA_PACKAGE;

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

    private ClassUtils() {
    }

    public static boolean isEnum(String type) {
        return type.contains(ENUM);
    }

    public static boolean isList(String type) {
        return type.contains(LIST_CLASS);
    }

    public static boolean isSet(String type) {
        return type.contains(SET_CLASS);
    }

    public static boolean isJavaBean(String type) {
        return !type.startsWith(JAVA_PACKAGE);
    }
}
