package core.framework.plugin.api.release.context;

import java.util.List;

/**
 * @author ebin
 */
public class MethodContext {
    public String name;
    public String params;
    public String returnType;
    public List<String> annotations;

    public MethodContext(String fieldName, List<String> annotations, String params, String returnType) {
        this.name = fieldName;
        this.annotations = annotations;
        this.params = params;
        this.returnType = returnType;
    }
}
