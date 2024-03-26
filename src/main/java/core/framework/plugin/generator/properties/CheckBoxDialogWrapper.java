package core.framework.plugin.generator.properties;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckBoxList;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ebin
 */
public class CheckBoxDialogWrapper extends DialogWrapper {
    private final CheckBoxList<JCheckBox> impactColumnCheckBox;
    public Set<Integer> selects = new HashSet<>();
    public boolean cancel = false;
    public List<String> selections;

    public CheckBoxDialogWrapper(String title, List<String> selections) {
        super(true); // use current window as parent
        setTitle(title);
        DefaultListModel<JCheckBox> model = new DefaultListModel<>();
        int index = 0;

        for (Object selection : selections) {
            JCheckBox checkBox = new JCheckBox(selection.toString());
            checkBox.setSelected(true);
            model.add(index, checkBox);
            selects.add(index);
            index++;
        }

        impactColumnCheckBox = new CheckBoxList<>(model, (i, value) -> {
            if (value) {
                selects.add(i);
            } else {
                selects.remove(i);
            }
        });
        setSize(800, 500);
        this.selections = selections;
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(impactColumnCheckBox, BorderLayout.CENTER);

        JScrollPane jScrollPane = new JScrollPane();
        jScrollPane.setSize(800, 500);
        jScrollPane.setAutoscrolls(true);
        jScrollPane.setViewportView(dialogPanel);
        return jScrollPane;
    }

    @Override
    public void doCancelAction() {
        cancel = true;
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}
