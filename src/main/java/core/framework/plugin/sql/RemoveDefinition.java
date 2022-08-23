package core.framework.plugin.sql;

import static core.framework.plugin.sql.BeanDefinition.COLUMN_NAME_FLAG;

/**
 * @author ebin
 */
public class RemoveDefinition {
    public static final String template = "SELECT COUNT(*) INTO @index FROM information_schema.`COLUMNS`\n" +
            "WHERE table_schema=DATABASE() AND table_name='%2$s' AND column_name='%1$s';\n" +
            "SET @SQL=IF(@index=1,'ALTER TABLE %2$s DROP COLUMN `%1$s`','select \\'Not Exists Column\\';');\n" +
            "PREPARE statement FROM @SQL;\n" +
            "EXECUTE statement;";

    public String columnName;

    public RemoveDefinition(String columnName) {
        this.columnName = columnName;
    }

    public String toAlertSql(String tableName) {
        String cname = columnName.replace(COLUMN_NAME_FLAG, "");
        return String.format(template, cname, tableName);
    }
}
