package core.framework.plugin;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl;
import com.intellij.ui.EditorTextField;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author ebin
 */
public class CopyEnumDialogWrapper extends DialogWrapper {
    EditorTextField textField;
    public String inputText;

    public CopyEnumDialogWrapper(String defaultName ) {
        super(true); // use current window as parent
        setTitle("Test DialogWrapper");
        textField = new EditorTextField(defaultName);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(textField, BorderLayout.CENTER);
        return dialogPanel;
    }

    @Override
    protected void doOKAction() {
        this.inputText = textField.getText();
        super.doOKAction();
    }
}