package core.framework.plugin.api.release.context;

import java.util.List;

/**
 * @author ebin
 */
public class FieldContext {
    public String name;
    public List<String> annotations;

    public FieldContext(String fieldName, List<String> annotations) {
        this.name = fieldName;
        this.annotations = annotations;
    }
}
