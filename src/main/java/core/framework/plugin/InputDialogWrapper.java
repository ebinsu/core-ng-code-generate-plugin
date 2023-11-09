package core.framework.plugin;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;

import javax.swing.*;
import java.awt.*;

/**
 * @author ebin
 */
public class InputDialogWrapper extends DialogWrapper {
    EditorTextField textField;
    public String inputText;

    public InputDialogWrapper(String defaultName) {
        super(true); // use current window as parent
        setTitle("Input");
        textField = new EditorTextField(defaultName);
        init();
    }

    public InputDialogWrapper(String title, String defaultName) {
        super(true); // use current window as parent
        setTitle(title);
        textField = new EditorTextField(defaultName);
        textField.setPreferredWidth(800);
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