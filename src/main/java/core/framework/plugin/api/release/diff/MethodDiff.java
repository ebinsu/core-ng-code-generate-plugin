package core.framework.plugin.api.release.diff;

import java.util.Objects;

/**
 * @author ebin
 */
public class MethodDiff {
    public AnnotationDiff annotationDiff;
    public String oldReturnType;
    public String newReturnType;

    public String oldParams;
    public String newParams;

    public boolean changeReturnType() {
        return !Objects.equals(oldReturnType, newReturnType);
    }

    public boolean changeParams() {
        return !Objects.equals(oldParams, newParams);
    }
}
