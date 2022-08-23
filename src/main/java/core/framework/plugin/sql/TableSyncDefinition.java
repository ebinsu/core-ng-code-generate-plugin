package core.framework.plugin.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ebin
 */
public class TableSyncDefinition {
    public List<AddDefinition> addDefinitions = new ArrayList<>();
    public List<RemoveDefinition> removeDefinitions = new ArrayList<>();
}
