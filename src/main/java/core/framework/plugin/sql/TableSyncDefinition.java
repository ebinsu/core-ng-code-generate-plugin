package core.framework.plugin.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ebin
 */
public class TableSyncDefinition {
    public List<AddDefinition> addDefinitions = new ArrayList<>();
    public List<UpdateDefinition> updateDefinitions = new ArrayList<>();
    public List<RemoveDefinition> removeDefinitions = new ArrayList<>();

    public List<SqlDefinition> getAll() {
        List<SqlDefinition> all = new ArrayList<>();
        all.addAll(addDefinitions);
        all.addAll(updateDefinitions);
        all.addAll(removeDefinitions);
        return all;
    }

    public TableSyncDefinition filter(List<Integer> selects) {
        List<SqlDefinition> all = new ArrayList<>();
        all.addAll(addDefinitions);
        all.addAll(updateDefinitions);
        all.addAll(removeDefinitions);
        TableSyncDefinition result = new TableSyncDefinition();
        selects.forEach(index -> {
            SqlDefinition sqlDefinition = all.get(index);
            if (sqlDefinition instanceof AddDefinition) {
                result.addDefinitions.add((AddDefinition) sqlDefinition);
            } else if (sqlDefinition instanceof UpdateDefinition) {
                result.updateDefinitions.add((UpdateDefinition) sqlDefinition);
            } else if (sqlDefinition instanceof RemoveDefinition) {
                result.removeDefinitions.add((RemoveDefinition) sqlDefinition);
            }
        });
        return result;
    }
}
