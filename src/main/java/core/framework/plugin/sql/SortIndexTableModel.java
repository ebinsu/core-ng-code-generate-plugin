package core.framework.plugin.sql;

import javax.swing.table.AbstractTableModel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author ebin
 */
public class SortIndexTableModel extends AbstractTableModel {
    private String[] columnNames = {"Column", "Order"};
    private Object[][] data;

    public SortIndexTableModel(Object[][] data) {
        this.data = data;
    }

    public List<String> getData() {
        return Arrays.stream(data).sorted(Comparator.comparingInt(o -> (Integer) o[1])).map(m -> (String) m[0]).toList();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1;
    }

    @Override
    public int getRowCount() {
        return data.length;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data[rowIndex][columnIndex];
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Integer newValue = Integer.parseInt(aValue.toString());
            Integer oldValue = (Integer) getValueAt(rowIndex, columnIndex);

            if (newValue.equals(oldValue)) {
                return;
            }

            performShiftUpdate(oldValue, newValue, rowIndex, columnIndex);

            fireTableDataChanged();

        } catch (NumberFormatException e) {
            System.err.println("Invalid number format: " + aValue);
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 1) {
            return Integer.class;
        }
        return String.class;
    }

    private void performShiftUpdate(Integer oldValue, Integer newValue, int changedRow, int changedCol) {
        for (int r = 0; r < getRowCount(); r++) {
            if (r == changedRow) {
                continue;
            }
            Integer cellValue = (Integer) getValueAt(r, 1);
            if (cellValue.compareTo(newValue) == 0) {
                data[r][1] = oldValue;
                break;
            }
        }

        data[changedRow][changedCol] = newValue;
        Arrays.sort(data, (row1, row2) -> {
            Integer value1 = (Integer) row1[1];
            Integer value2 = (Integer) row2[1];
            return value1.compareTo(value2);
        });
    }
}
