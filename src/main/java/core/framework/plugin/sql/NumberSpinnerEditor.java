package core.framework.plugin.sql;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;

/**
 * @author ebin
 */
public class NumberSpinnerEditor extends AbstractCellEditor implements TableCellEditor {
    private JSpinner spinner;
    private JSpinner.NumberEditor editor;
    private SpinnerNumberModel model;

    public NumberSpinnerEditor(int min, int max, int step) {
        // 创建数据模型，定义最小值、最大值和步长
        model = new SpinnerNumberModel(min, min, max, step);
        spinner = new JSpinner(model);
        // 设置编辑器的格式，例如不显示千位分隔符
        editor = new JSpinner.NumberEditor(spinner, "#");
        spinner.setEditor(editor);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // 当编辑开始时，将当前单元格的值设置到JSpinner中
        if (value instanceof Integer) {
            spinner.setValue(value);
        } else {
            spinner.setValue(0); // 默认值
        }
        return spinner;
    }

    @Override
    public Object getCellEditorValue() {
        // 获取编辑后的值，返回给表格模型
        return spinner.getValue();
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        // 确保单元格是可编辑的
        return true;
    }
}