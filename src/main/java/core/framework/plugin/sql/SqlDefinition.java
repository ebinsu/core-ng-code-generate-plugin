package core.framework.plugin.sql;

/**
 * @author ebin
 */
public interface SqlDefinition {
    String toAlertSql(String tableName);

    String getColumnName();

    String getDataType();

    String toString(int maxColumnNameLength, int maxDataTypeNameLength);
}
