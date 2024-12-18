package core.framework.plugin.sql;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author ebin
 */
public class DomainToSqlFileGenerator extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (project == null || virtualFile == null) {
            return;
        }
        while (!virtualFile.isDirectory()) {
            virtualFile = virtualFile.getParent();
        }
        TreeClassChooserFactory instance = TreeClassChooserFactory.getInstance(project);
        TreeClassChooser chooser = instance.createNoInnerClassesScopeChooser("Choose Domain To Generate Sql File.",
            GlobalSearchScope.projectScope(project),
            aClass -> {
                if (aClass.getQualifiedName() != null) {
                    return aClass.getQualifiedName().contains("domain");
                } else {
                    return false;
                }
            },
            null);
        chooser.showDialog();

        PsiClass mainClass = chooser.getSelected();
        if (mainClass == null) {
            return;
        }
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        BeanDefinition beanDefinition = new BeanDefinition(javaPsiFacade, mainClass);
        if (beanDefinition.tableName == null
            || beanDefinition.columns.isEmpty()) {
            return;
        }
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile);
        String filename = "R__" + beanDefinition.tableName + ".sql";
        PsiFile existsFile = directory.findFile(filename);
        if (existsFile == null) {
            PsiFile sqlFile = PsiFileFactory.getInstance(project).createFileFromText(filename, PlainTextFileType.INSTANCE, beanDefinition.toSql());
            WriteCommandAction.runWriteCommandAction(project, () -> {
                directory.add(sqlFile);
            });
        } else {
            Messages.showMessageDialog(filename + " existed.", "File Existed", Messages.getInformationIcon());
        }
    }
}
