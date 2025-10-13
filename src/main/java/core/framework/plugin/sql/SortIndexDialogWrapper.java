package core.framework.plugin.sql;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;

/**
 * @author ebin
 */
public class SortIndexDialogWrapper extends DialogWrapper {
    private final JBTable table;
    public boolean cancel = false;

    public SortIndexDialogWrapper(List<String> columns) {
        super(true); // use current window as parent
        setTitle("Adjust The Order Of Index Fields");

        Object[][] data = new Object[columns.size()][2];
        int index = 0;

        for (String column : columns) {
            data[index][0] = column;
            data[index][1] = index + 1;
            index++;
        }

        SortIndexTableModel tableModel = new SortIndexTableModel(data);

        table = new JBTable(tableModel);
        table.setRowHeight(30);
        TableColumn nameColumn = table.getColumnModel().getColumn(0);
        nameColumn.setPreferredWidth(Math.round(800 * 0.80f));
        table.getColumnModel().getColumn(1).setCellEditor(new NumberSpinnerEditor(0, columns.size(), 1));
        setSize(800, 500);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(table, BorderLayout.CENTER);
        JBScrollPane jScrollPane = new JBScrollPane();
        jScrollPane.setSize(800, 500);
        jScrollPane.setAutoscrolls(true);
        jScrollPane.setViewportView(dialogPanel);
        return jScrollPane;
    }

    public List<String> getData() {
        return ((SortIndexTableModel) table.getModel()).getData();
    }

    @Override
    public void doCancelAction() {
        cancel = true;
        super.doCancelAction();
    }
}
