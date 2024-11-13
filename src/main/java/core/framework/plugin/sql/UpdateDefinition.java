package core.framework.plugin.sql;

import java.util.stream.IntStream;

import static core.framework.plugin.sql.BeanDefinition.COLUMN_NAME_FLAG;
import static core.framework.plugin.sql.BeanDefinition.EMPTY_BLOCK;
import static core.framework.plugin.sql.BeanDefinition.LINE_START;
import static core.framework.plugin.sql.BeanDefinition.SINGLE_EMPTY_BLOCK;

/**
 * @author ebin
 */
public class UpdateDefinition implements SqlDefinition {
    public static final String template = "SELECT COUNT(*) INTO @index FROM information_schema.`COLUMNS`\n" +
        "WHERE table_schema=DATABASE() AND table_name='%1$s' AND column_name='%2$s';\n" +
        "SET @SQL=IF(@index>0,'ALTER TABLE %1$s MODIFY COLUMN %2$s %3$s %4$s','select \\'Exist Column\\';');\n" +
        "PREPARE statement FROM @SQL;\n" +
        "EXECUTE statement;";

    public String columnName;

    public String dateType;
    public String newConstraint;

    public UpdateDefinition(String columnName, String dateType, String newConstraint) {
        this.columnName = columnName;
        this.dateType = dateType;
        this.newConstraint = newConstraint;
    }

    public String toAlertSql(String tableName) {
        String cname = columnName.replace(COLUMN_NAME_FLAG, "");
        String formatConstraint = newConstraint;
        if (formatConstraint.contains("'")) {
            formatConstraint = formatConstraint.replace("'", "\\'");
        }
        return String.format(template, tableName, cname, dateType, formatConstraint);
    }

    public String toColumnSql(String formatReferenceText) {
        String cname = columnName;
        if (!columnName.contains(COLUMN_NAME_FLAG)) {
            cname = COLUMN_NAME_FLAG + columnName + COLUMN_NAME_FLAG;
        }
        int lineStartRepeatIndex = IntStream.range(0, formatReferenceText.length()).filter(i -> !Character.isWhitespace(formatReferenceText.charAt(i))).findFirst().orElse(-1);
        String lineStart;
        if (lineStartRepeatIndex > 0) {
            lineStart = SINGLE_EMPTY_BLOCK.repeat(lineStartRepeatIndex);
        } else {
            lineStart = LINE_START;
        }
        int nameToTypeRepeatIndex = -1;
        int columnNameEndIndex = IntStream.range(lineStartRepeatIndex, formatReferenceText.length()).filter(i -> Character.isWhitespace(formatReferenceText.charAt(i))).findFirst().orElse(-1);
        int typeStartIndex = -1;
        if (columnNameEndIndex != -1 && lineStartRepeatIndex != -1) {
            typeStartIndex = IntStream.range(columnNameEndIndex, formatReferenceText.length()).filter(i -> !Character.isWhitespace(formatReferenceText.charAt(i))).findFirst().orElse(-1);
            if (typeStartIndex != -1) {
                int length = typeStartIndex - lineStartRepeatIndex;
                nameToTypeRepeatIndex = length - cname.length();
            }
        }
        String nameToType;
        if (nameToTypeRepeatIndex > 0) {
            nameToType = SINGLE_EMPTY_BLOCK.repeat(nameToTypeRepeatIndex);
        } else {
            nameToType = EMPTY_BLOCK;
        }

        int typeToConstRepeatIndex = -1;
        String typeToConst;

        if (typeStartIndex != -1) {
            int typeEndIndex = IntStream.range(typeStartIndex, formatReferenceText.length()).filter(i -> Character.isWhitespace(formatReferenceText.charAt(i))).findFirst().orElse(-1);
            if (typeEndIndex != -1) {
                int constStartIndex = IntStream.range(typeEndIndex, formatReferenceText.length()).filter(i -> !Character.isWhitespace(formatReferenceText.charAt(i))).findFirst().orElse(-1);
                if (constStartIndex != -1) {
                    int length = constStartIndex - typeStartIndex;
                    typeToConstRepeatIndex = length - dateType.length();
                }
            }
        }

        if (typeToConstRepeatIndex > 0) {
            typeToConst = SINGLE_EMPTY_BLOCK.repeat(typeToConstRepeatIndex);
        } else {
            typeToConst = EMPTY_BLOCK;
        }

        return lineStart + cname + nameToType + dateType + typeToConst + newConstraint + ",";
    }

    @Override
    public String toString(int maxColumnNameLength, int maxDataTypeNameLength) {
        String cname = columnName;
        String formatDateType = dateType;
        return "Update column:" + EMPTY_BLOCK + cname + EMPTY_BLOCK + formatDateType + EMPTY_BLOCK + newConstraint;
    }

    @Override
    public String getColumnName() {
        return this.columnName;
    }

    @Override
    public String getDataType() {
        return this.dateType;
    }
}
