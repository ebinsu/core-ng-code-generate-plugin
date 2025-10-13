package core.framework.plugin.sql;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ebin
 */
public class SyncDomainToSqlDialogWrapper extends DialogWrapper {
    private final CheckBoxList<JCheckBox> impactColumnCheckBox;
    public Set<Integer> selects = new HashSet<>();
    public boolean cancel = false;

    public SyncDomainToSqlDialogWrapper(TableSyncDefinition tableSyncDefinition) {
        super(true); // use current window as parent
        setTitle("Choose Impact Columns");
        DefaultListModel<JCheckBox> model = new DefaultListModel<>();
        int index = 0;
        List<SqlDefinition> all = tableSyncDefinition.getAll();
        int maxColumnNameLength = all.stream().mapToInt(m -> m.getColumnName().length()).max().orElse(0);
        int maxDataTypeNameLength = all.stream().mapToInt(m -> m.getDataType().length()).max().orElse(0);

        for (SqlDefinition definition : all) {
            JCheckBox checkBox = new JCheckBox(definition.toString(maxColumnNameLength, maxDataTypeNameLength));
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
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(impactColumnCheckBox, BorderLayout.CENTER);

        JBScrollPane jScrollPane = new JBScrollPane();
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
