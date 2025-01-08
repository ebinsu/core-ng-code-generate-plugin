package core.framework.plugin.utils;

import org.apache.commons.io.FilenameUtils;

public final class FileUtils {
    public static boolean isSqlFile(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return extension.equals("sql");
    }

    public static boolean isJsonFile(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return extension.equals("json");
    }
}
