package core.framework.plugin.sql;

import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckBoxList;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ebin
 */
public class AddIndexDialogWrapper extends DialogWrapper {
    private final CheckBoxList<JCheckBox> columnCheckBox;
    public List<Integer> selects = new ArrayList<>();
    public boolean cancel = false;

    public AddIndexDialogWrapper(List<String> columns) {
        super(true); // use current window as parent
        setTitle("Select Column To Add To The Index");
        DefaultListModel<JCheckBox> model = new DefaultListModel<>();
        int index = 0;

        for (String column : columns) {
            JCheckBox checkBox = new JCheckBox(column);
            model.add(index, checkBox);
            index++;
        }

        columnCheckBox = new CheckBoxList<>(model, (i, value) -> {
            if (value) {
                if (!selects.contains(i)) {
                    selects.add(i);
                }
            } else {
                selects.remove(i);
            }
        });
        setSize(800, 500);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(columnCheckBox, BorderLayout.CENTER);
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
}
