package core.framework.plugin.sql;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLNotNullConstraint;
import com.alibaba.druid.sql.ast.statement.SQLNullConstraint;
import com.alibaba.druid.sql.ast.statement.SQLTableElement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.ide.util.TreeFileChooser;
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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import org.apache.commons.io.FilenameUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static core.framework.plugin.sql.BeanDefinition.COLUMN_NAME_FLAG;

/**
 * @author ebin
 */
public class SyncDomainToSqlGenerator extends AnAction {

    public static final String CREATE_TABLE_SCRIPT_END = "InnoDB;";

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (project == null || virtualFile == null) {
            return;
        }
        if (!isSqlFile(virtualFile.getName())) {
            return;
        }
        Document sqlDoc = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (sqlDoc == null) {
            return;
        }

        TreeClassChooserFactory instance = TreeClassChooserFactory.getInstance(project);
        TreeFileChooser chooser = instance.createFileChooser("Choose Domain To Generate Sql File.", null, JavaFileType.INSTANCE, null, true, false);
        chooser.showDialog();
        PsiFile psiFile = chooser.getSelectedFile();
        if (psiFile == null) {
            return;
        }
        PsiClass[] classes = ((PsiJavaFileImpl) psiFile).getClasses();
        if (classes.length == 0) {
            return;
        }
        PsiClass mainClass = classes[0];
        BeanDefinition beanDefinition = new BeanDefinition(mainClass);
        if (beanDefinition.tableName == null
                || beanDefinition.columns.isEmpty()) {
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

        if (!getRealName(currentStatement.getTableName()).equals(beanDefinition.tableName)) {
            Messages.showMessageDialog("Table names to be synchronized are not equal.", "Error", Messages.getErrorIcon());
            return;
        }

        String newSQL = beanDefinition.toSql();
        MySqlCreateTableStatement newStatement = null;
        try {
            SQLStatementParser newSQLParser = new MySqlStatementParser(newSQL);
            SQLStatement newSQLStatement = newSQLParser.parseStatement();
            if (newSQLStatement instanceof MySqlCreateTableStatement) {
                newStatement = (MySqlCreateTableStatement) newSQLStatement;
            } else {
                return;
            }
        } catch (Exception ex) {
            Messages.showMessageDialog("Parse sql for domain " + mainClass.getName() + " error.", "Error", Messages.getErrorIcon());
        }
        if (newStatement == null) {
            return;
        }

        TableSyncDefinition tableSyncDefinition = compare(currentStatement, newStatement);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            tableSyncDefinition.addDefinitions.forEach(addDefinition -> {
                int line = -1;
                if (addDefinition.afterColumnName != null) {
                    line = findLine(sqlDoc, addDefinition.afterColumnName);
                } else if (addDefinition.beforeFirstColumnName != null) {
                    line = findLine(sqlDoc, addDefinition.beforeFirstColumnName) - 1;
                }
                if (line != -1) {
                    sqlDoc.insertString(sqlDoc.getLineEndOffset(line), "\n");
                    sqlDoc.insertString(sqlDoc.getLineEndOffset(line + 1), addDefinition.toColumnSql());
                }
            });

            tableSyncDefinition.removeDefinitions.forEach(removeDefinition -> {
                int line = findLine(sqlDoc, removeDefinition.columnName);
                if (line != -1) {
                    sqlDoc.deleteString(sqlDoc.getLineStartOffset(line) - 1, sqlDoc.getLineEndOffset(line));
                }
            });

            tableSyncDefinition.addDefinitions.forEach(addDefinition -> {
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), "\n");
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), "\n");
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), addDefinition.toAlertSql(beanDefinition.tableName));
            });

            tableSyncDefinition.removeDefinitions.forEach(removeDefinition -> {
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), "\n");
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), "\n");
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), removeDefinition.toAlertSql(beanDefinition.tableName));
            });

            Messages.showMessageDialog("Sync finished.", "Success", Messages.getInformationIcon());
        });
    }

    private int findLine(Document sqlDoc, String searchText) {
        int lineCount = sqlDoc.getLineCount();
        for (int i = 1; i <= lineCount; i++) {
            String text = sqlDoc.getText(new TextRange(sqlDoc.getLineStartOffset(i), sqlDoc.getLineEndOffset(i)));
            if (text.contains(searchText)) {
                return i;
            }
        }
        return -1;
    }

    private TableSyncDefinition compare(MySqlCreateTableStatement currentStatement, MySqlCreateTableStatement newStatement) {
        TableSyncDefinition tableSyncDefinition = new TableSyncDefinition();
        List<SQLTableElement> currentTableElements = currentStatement.getTableElementList();
        List<SQLTableElement> newTableElements = newStatement.getTableElementList();

        compareColumn(tableSyncDefinition, currentTableElements, newTableElements);
        return tableSyncDefinition;
    }

    /*private void comparePrimaryKey(TableSyncDefinition tableSyncDefinition, List<SQLTableElement> currentTableElements, List<SQLTableElement> newTableElements) {
        Set<String> currentPKs = currentTableElements.stream().filter(f -> f instanceof MySqlPrimaryKey).map(m -> ((MySqlPrimaryKey) m).getName().getSimpleName()).collect(Collectors.toSet());
        Set<String> newPKs = newTableElements.stream().filter(f -> f instanceof MySqlPrimaryKey).map(m -> ((MySqlPrimaryKey) m).getName().getSimpleName()).collect(Collectors.toSet());
        if (!(newPKs.size() == currentPKs.size() && newPKs.containsAll(currentPKs))) {
            tableSyncDefinition.newPKs.addAll(newPKs);
        }
    }*/

    private void compareColumn(TableSyncDefinition tableSyncDefinition, List<SQLTableElement> currentTableElements, List<SQLTableElement> newTableElements) {
        List<SQLColumnDefinition> currentColumns = currentTableElements.stream().filter(f -> f instanceof SQLColumnDefinition).map(m -> (SQLColumnDefinition) m).collect(Collectors.toList());
        List<SQLColumnDefinition> newColumns = newTableElements.stream().filter(f -> f instanceof SQLColumnDefinition).map(m -> (SQLColumnDefinition) m).collect(Collectors.toList());
        String preColumnName = null;
        String currentFirstColumn = currentColumns.stream().findFirst().map(m -> m.getName().getSimpleName()).orElse(null);
        for (SQLColumnDefinition newColumn : newColumns) {
            String newColumnName = newColumn.getName().getSimpleName();
            Optional<SQLColumnDefinition> optional = currentColumns.stream().filter(f -> f.getName().getSimpleName().equals(newColumnName)).findFirst();
            if (optional.isEmpty()) {
                //add
                String constraint = newColumn.getConstraints().stream()
                        .filter(f -> f instanceof SQLNullConstraint || f instanceof SQLNotNullConstraint).findFirst()
                        .map(m -> m instanceof SQLNotNullConstraint ? "NOT NULL" : "NULL").orElse("NULL");
                tableSyncDefinition.addDefinitions.add(new AddDefinition(newColumnName, newColumn.getDataType().getName(), constraint, preColumnName, currentFirstColumn));
            } else {
                /*
                UpdateDefinition updateDefinition = new UpdateDefinition(newColumnName, preColumnName);
                SQLColumnDefinition currentColumn = optional.get();
                if (!newColumn.getDataType().equals(currentColumn.getDataType())) {
                    // type changed
                    updateDefinition.currentDateType = currentColumn.getDataType().toString();
                    updateDefinition.newDateType = newColumn.getDataType().toString();
                }
                Optional<SQLColumnConstraint> newConstraintOptional = newColumn.getConstraints().stream().filter(f -> f instanceof SQLNullConstraint || f instanceof SQLNotNullConstraint).findFirst();
                Optional<SQLColumnConstraint> currentConstraintOptional = currentColumn.getConstraints().stream().filter(f -> f instanceof SQLNullConstraint || f instanceof SQLNotNullConstraint).findFirst();
                if (newConstraintOptional.isPresent() && currentConstraintOptional.isPresent()) {
                    SQLColumnConstraint newConstraint = newConstraintOptional.get();
                    SQLColumnConstraint currentConstraint = currentConstraintOptional.get();
                    if (!newConstraint.getClass().equals(currentConstraint.getClass())) {
                        // constraint changed
                        updateDefinition.currentConstraint = currentConstraint instanceof SQLNotNullConstraint ? "NOT NULL" : "NULL";
                        updateDefinition.newConstraint = newConstraint instanceof SQLNotNullConstraint ? "NOT NULL" : "NULL";
                    }
                }
                if (updateDefinition.needUpdate()) {
                    tableSyncDefinition.updateDefinitions.add(updateDefinition);
                }*/
            }
            preColumnName = newColumnName;
        }

        for (SQLColumnDefinition currentColumn : currentColumns) {
            String currentColumnName = currentColumn.getName().getSimpleName();
            if (newColumns.stream().noneMatch(f -> f.getName().getSimpleName().equals(currentColumnName))) {
                tableSyncDefinition.removeDefinitions.add(new RemoveDefinition(currentColumnName));
            }
        }
    }

    private boolean isSqlFile(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return extension.equals("sql");
    }

    private String getRealName(String name) {
        return name.replace(COLUMN_NAME_FLAG, "");
    }
}
