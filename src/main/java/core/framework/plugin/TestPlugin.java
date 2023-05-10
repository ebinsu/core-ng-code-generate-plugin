package core.framework.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.panel.ProgressPanelBuilder;
import com.intellij.util.ui.UI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author ebin
 */
public class TestPlugin extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestDialogWrapper dialog = new TestDialogWrapper("x");
        dialog.show();
    }
}
