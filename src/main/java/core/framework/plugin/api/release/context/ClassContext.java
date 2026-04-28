package core.framework.plugin.api.release.context;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ebin
 */
public class ClassContext {
    public String className;
    public Type type;
    public List<FieldContext> fieldList = new ArrayList<>();
    public List<MethodContext> methodList = new ArrayList<>();
    public List<ClassContext> innerClasses = new ArrayList<>();

    public ClassContext(String className, String type) {
        this.className = className;
        this.type = Type.valueOf(type.toUpperCase());
    }

    public List<String> getFieldAnnotations(String filed) {
        return fieldList.stream().filter(f -> filed.equals(f.name)).findFirst().map(m -> m.annotations).orElse(List.of());
    }

    public List<String> getMethodAnnotations(String method) {
        return methodList.stream().filter(f -> method.equals(f.name)).findFirst().map(m -> m.annotations).orElse(List.of());
    }

    public MethodContext getMethod(String method) {
        return methodList.stream().filter(f -> method.equals(f.name)).findFirst().orElse(null);
    }

    public enum Type {
        INTERFACE,
        CLASS,
        ENUM
    }
}
