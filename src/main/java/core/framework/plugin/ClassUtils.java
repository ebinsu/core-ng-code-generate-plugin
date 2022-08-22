package core.framework.plugin;

import java.util.List;
import java.util.Set;

import static core.framework.plugin.PsiUtils.JAVA_PACKAGE;

/**
 * @author ebin
 */
public final class ClassUtils {
    public static final String ENUM_CLASS = Enum.class.getCanonicalName();
    public static final String STRING_CLASS = String.class.getCanonicalName();
    public static final String INTEGER_CLASS = Integer.class.getCanonicalName();
    public static final String DOUBLE_CLASS = Double.class.getCanonicalName();
    public static final String LIST_CLASS = List.class.getCanonicalName();
    public static final String SET_CLASS = Set.class.getCanonicalName();

    private ClassUtils() {
    }

    public static boolean isEnum(String type) {
        return type.contains(ENUM_CLASS);
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
