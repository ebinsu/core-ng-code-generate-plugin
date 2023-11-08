package core.framework.plugin.sql;

import java.util.Optional;

import static core.framework.plugin.sql.BeanDefinition.COLUMN_NAME_FLAG;
import static core.framework.plugin.sql.BeanDefinition.EMPTY_BLOCK;
import static core.framework.plugin.sql.BeanDefinition.LINE_START;

/**
 * @author ebin
 */
public class AddDefinition implements SqlDefinition {
    public static final String template = "SELECT count(*) INTO @index FROM information_schema.`COLUMNS`\n" +
        "WHERE table_schema = DATABASE() AND column_name = '%1$s' AND table_name = '%2$s';\n" +
        "SET @SQL = IF(@index = 0,\n" +
        " 'ALTER TABLE %2$s ADD COLUMN `%1$s` %3$s %4$s %5$s',\n" +
        " 'select \\'Not Alter Column\\';'\n" +
        ");\n" +
        "PREPARE statement FROM @SQL;\n" +
        "EXECUTE statement;";

    public String columnName;
    public String dateType;
    public String constraint;

    public String afterColumnName;
    public String beforeFirstColumnName;

    public AddDefinition(String columnName, String dateType, boolean notnull, String afterColumnName, String currentFirstColumn) {
        this.columnName = columnName;
        this.dateType = dateType;
        if (notnull) {
            if (dateType.contains(MysqlDialect.TIMESTAMP)) {
                this.constraint = "NOT NULL DEFAULT CURRENT_TIMESTAMP(6)";
            } else if (dateType.contains(MysqlDialect.BIT)) {
                this.constraint = "NOT NULL DEFAULT b'0'";
            } else {
                this.constraint = "NOT NULL";
            }
        } else {
            this.constraint = "NULL";
        }
        if (afterColumnName == null) {
            this.beforeFirstColumnName = currentFirstColumn;
        } else {
            this.afterColumnName = afterColumnName;
        }
    }

    public String toAlertSql(String tableName) {
        String cname = columnName.replace(COLUMN_NAME_FLAG, "");
        return String.format(template, cname, tableName, dateType, constraint,
            Optional.ofNullable(afterColumnName).map(name -> "AFTER " + name).orElse(""));
    }

    @Override
    public String getColumnName() {
        return this.columnName;
    }

    @Override
    public String getDataType() {
        return this.dateType;
    }

    public String toColumnSql() {
        String cname = columnName;
        if (!columnName.contains(COLUMN_NAME_FLAG)) {
            cname = COLUMN_NAME_FLAG + columnName + COLUMN_NAME_FLAG;
        }
        return LINE_START + cname + EMPTY_BLOCK + dateType + EMPTY_BLOCK + constraint + ",";
    }

    @Override
    public String toString(int maxColumnNameLength, int maxDataTypeNameLength) {
        String cname = columnName;
        String formatDateType = dateType;
        return "Add column:   " + EMPTY_BLOCK + cname + EMPTY_BLOCK + formatDateType + EMPTY_BLOCK + constraint;
    }
}
