package core.framework.plugin.api.release.diff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ebin
 */
public class ClassDiff {
    public String className;
    public Type type;
    public Map<String, List<String>> addFields = new LinkedHashMap<>(); // filed name / annotation
    public Map<String, List<String>> deleteFields = new LinkedHashMap<>(); // filed name / annotation
    public Map<String, AnnotationDiff> fieldChanges = new LinkedHashMap<>(); // filed name / annotation

    public Map<String, List<String>> addMethods = new LinkedHashMap<>(); // method name / annotation
    public Map<String, List<String>> deleteMethods = new LinkedHashMap<>(); // method name / annotation
    public Map<String, MethodDiff> methodChanges = new LinkedHashMap<>(); // method name / method diff

    public List<ClassDiff> innerClassDiffs = new ArrayList<>();

    public ClassDiff(String className, String type) {
        this.className = className;
        this.type = Type.valueOf(type.toUpperCase());
    }

    public enum Type {
        INTERFACE,
        CLASS,
        ENUM
    }
}
