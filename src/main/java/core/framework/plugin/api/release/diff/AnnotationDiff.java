package core.framework.plugin.api.release.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ebin
 */
public class AnnotationDiff {
    public List<String> addAnnotations = new ArrayList<>();
    public List<String> deleteAnnotations = new ArrayList<>();

    public boolean hasDiff() {
        return !addAnnotations.isEmpty() || !deleteAnnotations.isEmpty();
    }
}
