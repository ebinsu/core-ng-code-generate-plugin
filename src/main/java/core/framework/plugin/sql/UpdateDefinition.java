package core.framework.plugin.sql;

import static core.framework.plugin.sql.BeanDefinition.COLUMN_NAME_FLAG;
import static core.framework.plugin.sql.BeanDefinition.EMPTY_BLOCK;
import static core.framework.plugin.sql.BeanDefinition.LINE_START;

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
        return String.format(template, tableName, cname, dateType, newConstraint);
    }

    public String toColumnSql() {
        String cname = columnName;
        if (!columnName.contains(COLUMN_NAME_FLAG)) {
            cname = COLUMN_NAME_FLAG + columnName + COLUMN_NAME_FLAG;
        }
        return LINE_START + cname + EMPTY_BLOCK + dateType + EMPTY_BLOCK + newConstraint + ",";
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
