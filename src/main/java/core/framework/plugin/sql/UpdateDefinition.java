package core.framework.plugin.sql;

/**
 * @author ebin
 */
public class UpdateDefinition {
    public static final String template = "SELECT COUNT(*) INTO @index FROM information_schema.`COLUMNS`\n" +
            "WHERE table_schema=DATABASE() AND table_name='order_items' AND column_name='menu_item_name';\n" +
            "SET @SQL=IF(@index>0,'ALTER TABLE order_items MODIFY COLUMN menu_item_name VARCHAR(65)  NOT NULL','select \\'Exist Column\\';');\n" +
            "PREPARE statement FROM @SQL;\n" +
            "EXECUTE statement;";

    public String columnName;
    public String currentDateType;
    public String newDateType;

    public String currentConstraint;
    public String newConstraint;

    public String afterColumnName;

    public UpdateDefinition(String columnName, String afterColumnName) {
        this.columnName = columnName;
        this.afterColumnName = afterColumnName;
    }

    public boolean needUpdate() {
        return currentDateType != null || newDateType != null || currentConstraint != null || newConstraint != null;
    }

    public String toSql(String tableName) {
        return null;
    }
}
