package core.framework.plugin;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.EditorTextField;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

/**
 * @author ebin
 */
public class TestDialogWrapper extends DialogWrapper {
    CheckBoxList checkBoxList;
    public JCheckBox[] inputText;

    public TestDialogWrapper(String defaultName) {
        super(true); // use current window as parent
        setTitle("Test DialogWrapper");
        DefaultListModel stringDefaultListModel = new DefaultListModel<>();
        JCheckBox jCheckBox1 = new JCheckBox("a");
        JCheckBox jCheckBox2 = new JCheckBox("b");
        stringDefaultListModel.add(0, jCheckBox1);
        stringDefaultListModel.add(1, jCheckBox2);
        checkBoxList = new CheckBoxList<>(stringDefaultListModel,(index,value)->{
            System.out.printf("");
        });
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        dialogPanel.add(checkBoxList, BorderLayout.CENTER);
        return dialogPanel;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}