package core.framework.plugin.sql;

import core.framework.plugin.utils.ClassUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ebin
 */
public class MysqlDialect {
    public static final MysqlDialect INSTANCE = new MysqlDialect();
    public static final String VARCHAR = "VARCHAR";
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String BIT = "BIT";

    Map<String, String> types = new HashMap<>();

    private MysqlDialect() {
        types.put(ClassUtils.STRING, VARCHAR);
        types.put(ClassUtils.ENUM, "VARCHAR(100)");
        types.put(ClassUtils.LOCAL_DATE_TIME, "TIMESTAMP(6)");
        types.put(ClassUtils.ZONED_DATE_TIME, "TIMESTAMP(6)");
        types.put(ClassUtils.LOCAL_DATE, "DATE");
        types.put(ClassUtils.BOOLEAN, "BIT(1)");
        types.put(ClassUtils.INTEGER, "INT");
        types.put(ClassUtils.DOUBLE, "DECIMAL(10, 2)");
        types.put(ClassUtils.LONG, "BIGINT");
    }

    public String getType(String type, String columnName) {
        String mysqlType = types.get(type);
        if (mysqlType != null) {
            if (VARCHAR.equals(mysqlType)) {
                int size = 100;
                if (columnName.contains("ids")) {
                    size = 500;
                } else if (columnName.contains("note") || columnName.contains("reason")) {
                    size = 255;
                } else if (columnName.contains("id") || columnName.equals("created_by") || columnName.equals("updated_by")) {
                    size = 50;
                }
                return VARCHAR + "(" + size + ")";
            } else {
                return mysqlType;
            }
        } else {
            return "UNKNOWN";
        }
    }


}
