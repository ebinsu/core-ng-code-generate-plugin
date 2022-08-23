package core.framework.plugin;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.parser.SQLStatementParser;

/**
 * @author ebin
 */
public class SQLTest {
    public static void main(String[] args) {
        String sql = "CREATE TABLE IF NOT EXISTS `order_activities`\n" +
                "(\n" +
                "    `id`           VARCHAR(50)  NOT NULL,\n" +
                "    `order_id`     VARCHAR(50)  NOT NULL,\n" +
                "    `action`       VARCHAR(100) NOT NULL,\n" +
                "    `description`  VARCHAR(500) NULL,\n" +
                "    `operator`     VARCHAR(50)  NULL,\n" +
                "    `created_time` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),\n" +
                "    `created_by`   VARCHAR(50)  NOT NULL,\n" +
                "    PRIMARY KEY (`id`),\n" +
                "    KEY `ix_order_activities_order_id` (`order_id`)\n" +
                ") ENGINE = InnoDB;";
        SQLStatementParser parser = new MySqlStatementParser(sql);
        SQLStatement sqlStatement = parser.parseStatement();
        System.out.println(sqlStatement);
    }
}
