package core.framework.plugin.sql;

import com.alibaba.druid.sql.ast.SQLObjectImpl;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLTableElement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import core.framework.plugin.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ebin
 */
public class AddIndex extends AnAction {
    public static final String ADD_INDEX_TPL = """
            SELECT COUNT(*) INTO @index FROM information_schema.`STATISTICS`
            WHERE table_schema=DATABASE() AND table_name='%1$s' AND index_name = '%2$s';
            SET @SQL=IF(@index<1,'ALTER TABLE `%1$s` ADD INDEX `%2$s`(%3$s);','select \\'Index Not Exist\\';');
            PREPARE statement FROM @SQL;
            EXECUTE statement;
            """;

    public static final String INDEX_DEF = """
            INDEX `%1$s`(%2$s)
            """;
    public static final String COLUMN_NAME_FLAG = "`";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (project == null || virtualFile == null) {
            return;
        }
        if (!FileUtils.isSqlFile(virtualFile.getName())) {
            return;
        }
        Document sqlDoc = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (sqlDoc == null) {
            return;
        }

        String fullSql = sqlDoc.getText();
        MySqlCreateTableStatement currentStatement = null;
        try {
            SQLStatementParser currentSQLParser = new MySqlStatementParser(fullSql);
            SQLStatement currentSQLStatement = currentSQLParser.parseStatement();
            if (currentSQLStatement instanceof MySqlCreateTableStatement) {
                currentStatement = (MySqlCreateTableStatement) currentSQLStatement;
            } else {
                return;
            }
        } catch (Exception ex) {
            Messages.showMessageDialog("Parse " + virtualFile.getName() + " sql error.", "Error", Messages.getErrorIcon());
        }
        if (currentStatement == null) {
            return;
        }

        List<SQLTableElement> currentTableElements = currentStatement.getTableElementList();
        List<String> currentColumns = currentTableElements.stream().filter(f -> f instanceof SQLColumnDefinition).map(m -> ((SQLColumnDefinition) m).getColumnName()).toList();
        AddIndexDialogWrapper addIndexDialogWrapper = new AddIndexDialogWrapper(currentColumns);
        addIndexDialogWrapper.show();
        if (addIndexDialogWrapper.cancel) {
            return;
        }
        List<String> selects = addIndexDialogWrapper.selects.stream().map(currentColumns::get).toList();
        List<String> currentIndex = currentStatement.getMysqlIndexes().stream()
                .map(m -> m.getColumns().stream().map(SQLObjectImpl::toString).sorted().collect(Collectors.joining()))
                .toList();
        String addIndex = selects.stream().sorted().collect(Collectors.joining());
        if (currentIndex.contains(addIndex)) {
            Messages.showMessageDialog("Index already exists.", "Error", Messages.getErrorIcon());
            return;
        }

        String tableName = currentStatement.getTableName().replace(COLUMN_NAME_FLAG, "");
        String indexName = "ix_" + tableName + "_" + selects.stream().map(m -> m.replace(COLUMN_NAME_FLAG, "")).collect(Collectors.joining("_"));
        String indexDetail = selects.stream().map(m -> {
            if (m.contains(COLUMN_NAME_FLAG)) {
                return m;
            } else {
                return COLUMN_NAME_FLAG + m + COLUMN_NAME_FLAG;
            }
        }).collect(Collectors.joining(","));
        String indexSeg = String.format(ADD_INDEX_TPL, tableName, indexName, indexDetail);
        String indexDef = String.format(INDEX_DEF, tableName, indexDetail);
        int tableDefEndLine = findLine(sqlDoc, "InnoDB;");
        int line = findLastNonEmptyLine(sqlDoc, tableDefEndLine);

        WriteCommandAction.runWriteCommandAction(project, () -> {
            if (line != -1) {
                String text = sqlDoc.getText(new TextRange(sqlDoc.getLineStartOffset(line - 1), sqlDoc.getLineEndOffset(line - 1)));
                int leftPart = countLeftPart(text);
                StringBuilder def = new StringBuilder(indexDef);
                for (int i = 0; i <= leftPart; i++) {
                    def.insert(0, " ");
                }
                sqlDoc.insertString(sqlDoc.getLineEndOffset(line), ",");
                sqlDoc.insertString(sqlDoc.getLineStartOffset(line + 1), def.toString());

                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), "\n");
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), "\n");
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), indexSeg);
            }
        });
    }

    private int findLastNonEmptyLine(Document sqlDoc, int tableDefEndLine) {
        for (int i = tableDefEndLine - 1; i > 0; i--) {
            String text = sqlDoc.getText(new TextRange(sqlDoc.getLineStartOffset(i), sqlDoc.getLineEndOffset(i)));
            if (!StringUtils.isBlank(text)) {
                return i;
            }
        }
        return tableDefEndLine;
    }

    public int countLeftPart(String text) {
        int length = text.length();
        for (int i = 0; i < length; i++) {
            if (text.charAt(i) != 32) {
                return i - 1;
            }
        }
        return -1;
    }

    private int findLine(Document sqlDoc, String searchText) {
        int lineCount = sqlDoc.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            String text = sqlDoc.getText(new TextRange(sqlDoc.getLineStartOffset(i), sqlDoc.getLineEndOffset(i)));
            if (text.contains(searchText)) {
                return i;
            }
        }
        return -1;
    }
}
