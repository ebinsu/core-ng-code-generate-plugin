package core.framework.plugin;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import java.awt.*;

/**
 * @author ebin
 */
public class TextAreaDialogWrapper extends DialogWrapper {
    JBTextArea textField;
    public String inputText;

    public TextAreaDialogWrapper(String title, String defaultName) {
        super(true); // use current window as parent
        setTitle(title);
        textField = new JBTextArea(defaultName);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(textField, BorderLayout.CENTER);
        dialogPanel.setPreferredSize(new Dimension(800, 500));
        return dialogPanel;
    }

    @Override
    protected void doOKAction() {
        this.inputText = textField.getText();
        super.doOKAction();
    }
}