package core.framework.plugin.utils;

/**
 * @author ebin
 */
public class StringUtils {
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
