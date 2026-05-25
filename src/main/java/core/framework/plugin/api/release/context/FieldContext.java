package core.framework.plugin.api.release.context;

import java.util.List;

/**
 * @author ebin
 */
public class FieldContext {
    public String name;
    public List<String> annotations;
    public boolean hasDefaultValue;

    public FieldContext(String fieldName, List<String> annotations, boolean hasDefaultValue) {
        this.name = fieldName;
        this.annotations = annotations;
        this.hasDefaultValue = hasDefaultValue;
    }
}
