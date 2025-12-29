package core.framework.plugin.generator.format;

/**
 * @author ebin
 */
public class StringConverterUtils {
    public static String toLowerCamel(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        // 1. 驼峰拆词：PublishedMenuItem → Published Menu Item
        String step1 = input.replaceAll(
            "([a-z])([A-Z])", "$1 $2"
        );

        // 2. 分隔符统一为空格
        String step2 = step1.replaceAll("[\\s_-]+", " ");

        // 3. 全部转小写并拆词
        String[] words = step2.toLowerCase().trim().split(" ");

        if (words.length == 0) {
            return "";
        }

        // 4. 组装 lowerCamelCase
        StringBuilder result = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            result.append(capitalize(words[i]));
        }

        return result.toString();
    }

    public static String toUnderLine(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        // 1. 驼峰拆分：aB -> a B
        String step1 = input.replaceAll(
            "([a-z])([A-Z])", "$1 $2"
        );

        // 2. 统一分隔符为空格（空格、-、_）
        String step2 = step1.replaceAll("[\\s_-]+", " ");

        // 3. 转小写并用下划线拼接
        return step2
            .trim()
            .toLowerCase()
            .replace(" ", "_");
    }

    public static String toMiddleLine(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        // 1. 驼峰拆分：aB -> a B
        String step1 = input.replaceAll(
            "([a-z])([A-Z])", "$1 $2"
        );

        // 2. 统一分隔符（空格、_、-）为空格
        String step2 = step1.replaceAll("[\\s_-]+", " ");

        // 3. 转小写并用 '-' 拼接
        return step2
            .trim()
            .toLowerCase()
            .replace(" ", "-");
    }

    public static String toLowerSpace(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        // 1. 驼峰拆分：aB -> a B
        String step1 = input.replaceAll(
            "([a-z])([A-Z])", "$1 $2"
        );

        // 2. 统一分隔符（空格、_、-）为空格
        String step2 = step1.replaceAll("[\\s_-]+", " ");

        // 3. 转小写并去掉多余空格
        return step2
            .trim()
            .toLowerCase();
    }

    private static String capitalize(String word) {
        if (word.isEmpty()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

}
