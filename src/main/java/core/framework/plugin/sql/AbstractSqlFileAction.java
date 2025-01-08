package core.framework.plugin.sql;

import com.intellij.ide.actions.EditSourceAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import core.framework.plugin.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author ebin
 */
public abstract class AbstractSqlFileAction extends EditSourceAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setVisible(isClickSqlFile(e));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private boolean isClickSqlFile(AnActionEvent e) {
        Project project = getEventProject(e);
        Module module = e.getData(PlatformCoreDataKeys.MODULE);
        if (project != null && module != null) {
            VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
            if (file != null) {
                return FileUtils.isSqlFile(file.getName());
            }
        }
        return false;
    }
}
