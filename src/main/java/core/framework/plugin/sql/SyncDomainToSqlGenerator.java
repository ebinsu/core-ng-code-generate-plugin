package core.framework.plugin.sql;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLColumnConstraint;
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
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import org.apache.commons.io.FilenameUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        BeanDefinition beanDefinition = new BeanDefinition(javaPsiFacade, mainClass);
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

        TableSyncDefinition tableSyncDefinition = compare(currentStatement, beanDefinition);
        SyncDomainToSqlDialogWrapper syncDomainToSqlDialogWrapper = new SyncDomainToSqlDialogWrapper(tableSyncDefinition);
        syncDomainToSqlDialogWrapper.show();
        if (syncDomainToSqlDialogWrapper.cancel) {
            return;
        }
        List<Integer> selects = syncDomainToSqlDialogWrapper.selects.stream().sorted().toList();
        TableSyncDefinition finalTableSyncDefinition = tableSyncDefinition.filter(selects);

        WriteCommandAction.runWriteCommandAction(project, () -> {
            finalTableSyncDefinition.addDefinitions.forEach(addDefinition -> {
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

            finalTableSyncDefinition.updateDefinitions.forEach(updateDefinition -> {
                int line = findLine(sqlDoc, updateDefinition.columnName);
                if (line != -1) {
                    sqlDoc.deleteString(sqlDoc.getLineStartOffset(line), sqlDoc.getLineEndOffset(line));
                    sqlDoc.insertString(sqlDoc.getLineEndOffset(line), updateDefinition.toColumnSql());
                }
            });

            finalTableSyncDefinition.removeDefinitions.forEach(removeDefinition -> {
                int line = findLine(sqlDoc, removeDefinition.columnName);
                if (line != -1) {
                    sqlDoc.deleteString(sqlDoc.getLineStartOffset(line) - 1, sqlDoc.getLineEndOffset(line));
                }
            });

            finalTableSyncDefinition.addDefinitions.forEach(addDefinition -> {
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), "\n");
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), "\n");
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), addDefinition.toAlertSql(beanDefinition.tableName));
            });

            finalTableSyncDefinition.updateDefinitions.forEach(updateDefinition -> {
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), "\n");
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), "\n");
                sqlDoc.insertString(sqlDoc.getLineEndOffset(sqlDoc.getLineCount() - 1), updateDefinition.toAlertSql(beanDefinition.tableName));
            });

            finalTableSyncDefinition.removeDefinitions.forEach(removeDefinition -> {
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

    private TableSyncDefinition compare(MySqlCreateTableStatement currentStatement, BeanDefinition newStatement) {
        TableSyncDefinition tableSyncDefinition = new TableSyncDefinition();
        List<SQLTableElement> currentTableElements = currentStatement.getTableElementList();
        Set<String> newColumns = newStatement.columns.keySet();

        List<SQLColumnDefinition> currentColumns = currentTableElements.stream().filter(f -> f instanceof SQLColumnDefinition).map(m -> (SQLColumnDefinition) m).collect(Collectors.toList());
        String preColumnName = null;
        String currentFirstColumn = currentColumns.stream().findFirst().map(m -> m.getName().getSimpleName()).orElse(null);
        for (String newColumn : newColumns) {
            Optional<SQLColumnDefinition> optional = currentColumns.stream().filter(f -> getRealName(f.getName().getSimpleName()).equals(newColumn)).findFirst();
            if (optional.isEmpty()) {
                //add
                tableSyncDefinition.addDefinitions.add(new AddDefinition(newColumn, newStatement.columns.get(newColumn), newStatement.notNullFields.contains(newColumn), preColumnName, currentFirstColumn));
            } else {
                // update
                if (!newStatement.primaryKeys.contains(newColumn)) {
                    SQLColumnDefinition currentColumn = optional.get();

                    Optional<SQLColumnConstraint> currentConstraintOptional = currentColumn.getConstraints().stream().filter(f -> f instanceof SQLNullConstraint || f instanceof SQLNotNullConstraint).findFirst();
                    if (currentConstraintOptional.isPresent()) {
                        SQLColumnConstraint currentConstraint = currentConstraintOptional.get();
                        boolean currentIsNotNull = currentConstraint instanceof SQLNotNullConstraint;
                        boolean newIsNotNull = newStatement.notNullFields.contains(newColumn);
                        if (newIsNotNull != currentIsNotNull) {
                            UpdateDefinition updateDefinition = new UpdateDefinition(newColumn, currentColumn.getDataType().toString(), newIsNotNull ? "NOT NULL" : "NULL");
                            tableSyncDefinition.updateDefinitions.add(updateDefinition);
                        }
                    }
                }
            }
            preColumnName = newColumn;
        }

        for (SQLColumnDefinition currentColumn : currentColumns) {
            String currentColumnName = getRealName(currentColumn.getName().getSimpleName());
            if (newColumns.stream().noneMatch(f -> f.equals(currentColumnName))) {
                tableSyncDefinition.removeDefinitions.add(new RemoveDefinition(currentColumnName));
            }
        }
        return tableSyncDefinition;
    }

    private boolean isSqlFile(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return extension.equals("sql");
    }

    private String getRealName(String name) {
        return name.replace(COLUMN_NAME_FLAG, "");
    }
}
